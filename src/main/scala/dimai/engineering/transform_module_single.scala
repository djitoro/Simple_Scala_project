package dimai.engineering
import org.apache.spark.sql.{Column, DataFrame, SparkSession, functions => F}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._

object transform_module_single {
  def buildTable(spark: SparkSession, allLines: Array[String]): DataFrame = {
    import spark.implicits._

    val df = spark.createDataFrame(allLines.map(Tuple1(_))).toDF("log")
    val filteredDF = df.filter(!$"log".rlike("^\\$0"))

    val dfWithEventAndDate = filteredDF
      .withColumn("event_type", F.regexp_extract($"log", "^(QS|CARD_SEARCH_START|CARD_SEARCH_END|DOC_OPEN|SESSION_START|SESSION_END)", 1))
      .withColumn("date", F.regexp_extract($"log", "(\\d{2}\\.\\d{2}\\.\\d{4}_\\d{2}:\\d{2}:\\d{2})", 1))

    val dfWithDocumentIds = dfWithEventAndDate
      .withColumn("document_ids", F.split(F.regexp_extract($"log", "(\\b[A-Z]+_\\d+\\b.*)", 1), " "))
      .withColumn("document_id", F.explode($"document_ids"))
      .withColumn("is_doc_open", $"event_type" === "DOC_OPEN")
      .withColumn("search_type", F.when($"event_type" === "QS", "быстрый поиск")
        .when($"event_type" === "CARD_SEARCH_START", "поиск по карточке")
        .otherwise(null))
      .withColumn("search_query", F.when($"event_type" === "QS", F.regexp_extract($"log", "\\{([^}]+)\\}", 1))
        .when($"event_type" === "CARD_SEARCH_START", F.regexp_extract($"log", "\\$\\d+\\s+(.+)", 1))
        .otherwise(null))

    val dfWithQuery = dfWithDocumentIds.withColumn("query_temp",
      when(col("log").rlike("^\\$\\d+"), regexp_extract(col("log"), "^\\$\\d+\\s+(.+)", 1)))

    val result = dfWithQuery.select("document_id", "date", "search_type", "is_doc_open", "search_query", "query_temp")

    val resultWithRowNumbers = result.withColumn("row_number", monotonically_increasing_id())

    val windowSpec = Window.orderBy("row_number").rowsBetween(Window.unboundedPreceding, Window.currentRow)
    val filledDF = resultWithRowNumbers
      .withColumn("search_type", last($"search_type", ignoreNulls = true).over(windowSpec))
      .withColumn("search_query", coalesce($"search_query", $"query_temp"))
      .withColumn("search_query", last($"search_query", ignoreNulls = true).over(windowSpec))

    val formattedData = filledDF.withColumn("opening_timestamp", to_timestamp(col("date"), "dd.MM.yyyy_HH:mm:ss"))
    val dataWithDate = formattedData.withColumn("opening_date", to_date(col("opening_timestamp")))
    dataWithDate.show()
    val sessionWindow = Window.orderBy("row_number").rowsBetween(Window.unboundedPreceding, Window.currentRow)
    val dataWithFixedDate = dataWithDate.withColumn("opening_date",
      last(when(col("is_doc_open"), col("opening_date")), ignoreNulls = true).over(sessionWindow))

    val dataWithFixedDate_clean = dataWithFixedDate.distinct()
    val dataWithDate_ = dataWithFixedDate_clean.select("document_id", "search_type", "is_doc_open", "search_query", "opening_date")

    val cleanedDF = dataWithDate_
      .withColumn("document_id", when(trim(col("document_id")) === "", lit(null)).otherwise(col("document_id")))
      .withColumn("search_type", when(trim(col("search_type")) === "", lit(null)).otherwise(col("search_type")))
      .withColumn("search_query", when(trim(col("search_query")) === "", lit(null)).otherwise(col("search_query")))
      .na.drop(Seq("document_id"))

    val partitionedDF = cleanedDF.repartition($"opening_date")
    val partitionedDF_ = partitionedDF.filter(col("is_doc_open") === false)
    partitionedDF_.show(50)
    partitionedDF
  }
}