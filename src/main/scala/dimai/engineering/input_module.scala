package dimai.engineering

import scala.io.Source
import java.nio.charset.Charset
import scala.collection.mutable.ArrayBuffer


object input_module {
  def buildArray(): Array[String] = {

    // Укажите путь к вашему текстовому файлу
    val baseFilePath = "/Users/dimai/Downloads/task_de/session/"

    // Количество файлов
    val totalFiles = 9999
    // Создаем ArrayBuffer для накопления строк
    val allLines: ArrayBuffer[String] = ArrayBuffer()

    // Итерируемся по номерам файлов
    for (i <- 1 to totalFiles) {
      // Формируем путь к текущему файлу
      val filePath = s"$baseFilePath$i"

      // Указываем кодировку файла (например, Windows-1251)
      val source = Source.fromFile(filePath)(Charset.forName("Windows-1251"))
      try {
        // Читаем файл и добавляем его строки в общий массив
        val linesArray: Array[String] = source.getLines().toArray
        allLines ++= linesArray

      } catch {
        // Обрабатываем возможные ошибки (например, если файл не существует)
        case e: Exception => println(s"Ошибка при чтении файла $i: ${e.getMessage}")
      } finally {
        // Закрываем файл
        source.close()
      }
    }

    // Преобразуем ArrayBuffer в массив (если это необходимо)
    val finalArray: Array[String] = allLines.toArray

    // Выводим общее количество строк
    println(s"Общее количество строк во всех файлах: " + finalArray.length)

    finalArray
  }
}
