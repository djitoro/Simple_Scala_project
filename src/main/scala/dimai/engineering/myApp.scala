package dimai.engineering

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._


object myApp {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("Spark test task").master("local[1]").getOrCreate()

    val allLines: Array[String] = input_module.buildArray()

    val df = transform_module.buildTable(spark, allLines)

    // 1:
    val count = df
      .where(
          (col("search_type") === "поиск по карточке") &&
          (col("is_doc_open") === false) &&
          (col("document_id") === "ACC_45616")
      )
      .count()
    println(s"Количество строк, удовлетворяющих условиям: $count")

    // 2:
    val df_counter_doc = counter_doc.buildTable(spark, df)
    // Выводим результат
    df_counter_doc.show() // выставить число более 175611 для вывода информации о всех документах

    spark.stop()

  }
}