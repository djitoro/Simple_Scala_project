# This is my first Scala project.
(V2)
___

# What has changed
(A lot of things)

## New struct: 
![table](https://github.com/djitoro/Simple_Scala_project/blob/main/Table_for_Scala_2.png)

List of changes: 
- added information about the request
- improved separation by dates and
- taken into account the differences in strucutre different files

## Modified data handling: 
List of changes: 
- Added multithreading
- added resistance to duplicates

## Dealing with anomalies:
- Empty queries are ignored
- Technical breakdowns are not taken into account
- checked the distribution of data


Processing one batch and distributed into several batches gave different results, I'll try to figure out why a little later, but for now I'll just add both versions of the code and both answers.
  
Multithreaded: 1095
Single-threaded: 468
___


## Main steps: 

### 0. Installing components
  Most of the time was spent installing components, configuring dependencies, and coordinating different parts.
  
  On the plus side: I began to understand Indians


### 1. Building a table
  The initial data was in an inconvenient text format, for simple and convenient operation it was necessary to create a table with basic data.


  That's what I did: 
  
  ![table](https://github.com/djitoro/Simple_Scala_project/blob/main/Table_for_Scala.png)
      
      table based on text data
      
 That's about how I did it: 
  ```Scala
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
  ```

### 2. first task
  Answer: 503

  That's how it was received:
  ```Scala
    val count = df
      .where(
          (col("search_type") === "поиск по карточке") &&
          (col("is_doc_open") === false) &&
          (col("document_id") === "ACC_45616")
      )
      .count()
    println(s"Количество строк, удовлетворяющих условиям: $count")
  ```

  
### 3. second task
  Answer: Spark table 
  
  That's how it was received:
  ```Scala
    // Преобразуем строку времени в Timestamp
    val formattedData = df.withColumn(
      "opening_timestamp",
      to_timestamp(col("date"), "dd.MM.yyyy_HH:mm:ss"))

    // Извлекаем дату из Timestamp
    val dataWithDate = formattedData.withColumn(
      "opening_date",
      to_date(col("opening_timestamp")))

    // Группируем по названию документа и дате, затем считаем количество открытий
    val result = dataWithDate
      .groupBy(col("document_id"), col("opening_date"))
      .agg(count("*").as("openings_count"))
  ```

___
## Ideas for improvement: 
### Date: 
For other tasks, you will need to modernize the table and add information about the query text to it. Error handling can also be improved.

### Config: 
For heavy loads, you need to configure the Spark configuration.
___
