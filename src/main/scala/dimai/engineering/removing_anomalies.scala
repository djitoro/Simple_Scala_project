package dimai.engineering

import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object removing_anomalies {
  def remove_anomalies(spark: SparkSession, df: DataFrame): DataFrame = {
    import spark.implicits._

    // отсечение сессий, в которых ничего не открывалось
    val docOpenCounts = df.filter($"is_doc_open").groupBy("search_type").count()
    val validDocOpenSessions = docOpenCounts.filter(col("count") > 0).select("search_type")
    val filteredDF1 = df.join(validDocOpenSessions, Seq("search_type"))
    // отсечение сессий, в которых не было поисков
    val queryCounts = filteredDF1.groupBy("search_query").count()
    val validQueries = queryCounts.filter(col("count") > 0).select("search_query")
    val filteredDF2 = filteredDF1.join(validQueries, Seq("search_query"))
    // отсечение сессий, в которых не открывались документы
    val docCounts = filteredDF2.groupBy("document_id").count()
    val validDocs = docCounts.filter(col("count") > 0).select("document_id")
    filteredDF2.join(validDocs, Seq("document_id"))

    filteredDF2
  }
}
