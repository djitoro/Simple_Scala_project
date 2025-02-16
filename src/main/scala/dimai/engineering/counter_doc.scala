package dimai.engineering

import org.apache.spark.sql.functions.{col, count, to_date, to_timestamp}
import org.apache.spark.sql.{DataFrame, SparkSession}

object counter_doc {
  def buildTable(spark: SparkSession, df: DataFrame): DataFrame = {

    // Преобразуем строку времени в Timestamp
    val formattedData = df.withColumn(
      "opening_timestamp",
      to_timestamp(col("date"), "dd.MM.yyyy_HH:mm:ss"))

    // Извлекаем дату из Timestamp
    val dataWithDate = formattedData.withColumn(
      "opening_date",
      to_date(col("opening_timestamp"))
    )

    // Группируем по названию документа и дате, затем считаем количество открытий
    val result = dataWithDate
      .groupBy(col("document_id"), col("opening_date"))
      .agg(count("*").as("openings_count"))

    result
  }
}