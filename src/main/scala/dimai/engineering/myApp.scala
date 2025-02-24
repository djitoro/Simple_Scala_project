package dimai.engineering

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._


object myApp {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("Spark test task").master("local[1]").getOrCreate()

    val allLines: Array[String] = input_module.buildArray()

    val df = transform_module.buildTable(spark, allLines)

    val df_clean = removing_anomalies.remove_anomalies(spark, df)
    // 1:
    // обработка очень простая и уже довольно эффективна
    val count_df = df_clean
      .where(
          (col("search_type") === "поиск по карточке") &&
          (col("is_doc_open") === false) &&
          (col("document_id") === "ACC_45616")
      )
      .count()
    println(s"Количество строк, удовлетворяющих условиям: $count_df")
    
    // 2:
    // добавлено партиционирование по дате - это повысит скорость работы
    val df_counter_doc = counter_doc.buildTable(spark, df_clean)
    // Выводим результат
    val df_counter_doc_desc = df_counter_doc.orderBy(desc("openings_count"))
    df_counter_doc_desc.show(50) // выставить число более 175611 для вывода информации о всех документах

    spark.stop()
  }
}
