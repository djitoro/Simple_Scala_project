package dimai.engineering

import org.apache.spark.sql.functions.{col, count, to_date, to_timestamp}
import org.apache.spark.sql.{DataFrame, SparkSession}

object counter_doc {
  def buildTable(spark: SparkSession, df: DataFrame): DataFrame = {

    val result = df
      .groupBy(col("document_id"), col("opening_date"))
      .agg(count("*").as("openings_count"))

    result
  }
}
