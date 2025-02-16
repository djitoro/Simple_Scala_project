package dimai.engineering

import org.apache.spark.sql.{Column, DataFrame, SparkSession, functions => F}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._

object transform_module {
  def buildTable(spark: SparkSession, allLines: Array[String]): DataFrame = {
    import spark.implicits._

    // Создание DataFrame
    val df = spark.createDataFrame(allLines.map(Tuple1(_))).toDF("log")

    // Извлечение типа события и даты
    val dfWithEventAndDate = df
      .withColumn("event_type", F.regexp_extract($"log", "^(QS|CARD_SEARCH_START|CARD_SEARCH_END|DOC_OPEN|SESSION_START|SESSION_END)", 1))
      .withColumn("date", F.regexp_extract($"log", "(\\d{2}\\.\\d{2}\\.\\d{4}_\\d{2}:\\d{2}:\\d{2})", 1))

    // Извлечение всех идентификаторов документов
    val dfWithDocumentIds = dfWithEventAndDate
      .withColumn("document_ids", F.split(F.regexp_extract($"log", "(\\b[A-Z]+_\\d+\\b.*)", 1), " "))
      .withColumn("document_id", F.explode($"document_ids"))
      .withColumn("is_doc_open", $"event_type" === "DOC_OPEN")
      .withColumn("search_type",
        F.when($"event_type" === "QS", "быстрый поиск")
          .when($"event_type" === "CARD_SEARCH_START", "поиск по карточке")
          .otherwise(null))

    // Фильтрация и выбор нужных столбцов
    val result = dfWithDocumentIds.select("document_id", "date", "search_type", "is_doc_open")

    // Добавляем номера строк
    val resultWithRowNumbers = result.withColumn("row_number", monotonically_increasing_id())

    // Заполнение значений NULL в столбце search_type последним не NULL значением
    val windowSpec = Window.orderBy("row_number").rowsBetween(Window.unboundedPreceding, Window.currentRow)
    val filledDF = resultWithRowNumbers.withColumn(
      "search_type",
      last($"search_type", ignoreNulls = true).over(windowSpec)
    )

    filledDF
  }
}
