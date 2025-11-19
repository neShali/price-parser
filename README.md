# price-parser

Учебный многопоточный парсер цен на товары на Spring Boot.

Проект демонстрирует:

- работу с **Spring Boot 3**, **JPA/Hibernate** и **H2** (in-memory БД);
- многопоточную обработку задач через **ExecutorService** и `@Scheduled`;
- использование **WebClient (WebFlux)** для получения дополнительных данных о товаре;
- параллельную фильтрацию результатов (`parallelStream()`);
- базовые **unit-тесты** контроллера и сервисов.

---

## 1. Как запустить

В корне проекта:

    mvn spring-boot:run

Приложение стартует на: `http://localhost:8080`.


## 2. База данных и H2 Console

Используется **in-memory H2** (настройки смотрите в `application.properties`):

- URL: `jdbc:h2:mem:priceparserdb`
- user: `sa`
- password: пустой
- консоль H2 включена по адресу: `http://localhost:8080/h2-console`

При подключении в консоли:

- JDBC URL: `jdbc:h2:mem:priceparserdb`
- User Name: `sa`
- Password: (пусто)

Основные таблицы:

- `PARSING_TASKS` — задачи на парсинг
- `PRODUCTS` — распарсенные товары

При старте приложения в таблицу `PARSING_TASKS` добавляются 5 демо-задач (`DataInitializer`).

---

## 3. Архитектура

### 3.1. Доменные сущности

**Product**

- `id` — PK
- `name` — название товара
- `description` — описание
- `price` — цена (`BigDecimal`)
- `publicationDate` — дата публикации
- `sourceUrl` — URL страницы товара

**ParsingTask**

- `id` — PK
- `targetUrl` — URL, который надо распарсить
- `status` — `NEW`, `IN_PROGRESS`, `COMPLETED`, `FAILED`
- `errorMessage` — сообщение об ошибке при парсинге (если есть)
- `createdAt`, `updatedAt` — технические поля

---

### 3.2. Слои и сервисы

- `PriceParsingService`  
  Заглушка парсера: по URL генерирует тестовый `Product`.  
  Имитация реального HTTP-запроса и разбора HTML. При желании здесь можно подключить Jsoup.

- `ParsingTaskProcessingService`  
  Многопоточная обработка задач парсинга:
    - загружает задачи со статусом `NEW`;
    - ставит им статус `IN_PROGRESS`;
    - в пуле потоков вызывает `PriceParsingService.parseProduct(...)`;
    - сохраняет `Product` и переводит задачу в `COMPLETED` или `FAILED`.

- `ParsingExecutorConfig`  
  Конфигурация `ExecutorService` (пул потоков для парсинга).

- `ParsingScheduler`  
  Класс с `@Scheduled`, который периодически:
    - ищет новые задачи;
    - раздаёт их в пул потоков через `ParsingTaskProcessingService`.

- `ProductQueryService`  
  Получение списка товаров с использованием `parallelStream()`:
    - фильтрация по имени и диапазону цен;
    - сортировка (по цене, имени, дате публикации);
    - пагинация.

- `PriceParserController`  
  REST-контроллер с эндпоинтами:
    - `POST /parse` — добавить задачу парсинга;
    - `GET /products` — получить страницу товаров из БД;
    - `GET /products/filtered` — получить отфильтрованные товары с параллельной обработкой.

- `WebClientConfig` и `ExternalProductInfoClient`  
  Пример взаимодействия с внешним сервисом через `WebClient` (обогащение данных о товаре).

---

## 4. REST API

### 4.1. Добавить URL для парсинга

**POST** `/parse`

Тело запроса:

    {
      "url": "https://example.com/product/999"
    }

Пример через `curl` (Git Bash / WSL):

    curl -X POST "http://localhost:8080/parse" \
      -H "Content-Type: application/json" \
      -d "{\"url\": \"https://example.com/product/999\"}"

Ответ (пример):

    {
      "id": 6,
      "url": "https://example.com/product/999",
      "status": "NEW",
      "errorMessage": null,
      "createdAt": "2025-11-19T00:01:53.2536871",
      "updatedAt": "2025-11-19T00:01:53.2536871"
    }

После этого задача попадёт в таблицу `PARSING_TASKS`.  
`ParsingScheduler` найдёт её и отправит в пул потоков на обработку.

---

### 4.2. Получить товары (простая пагинация)

**GET** `/products`

Параметры пагинации:

- `page` — номер страницы (0-based, по умолчанию `0`)
- `size` — размер страницы (по умолчанию `20`)
- `sort` — сортировка, например:
    - `price,desc`
    - `name,asc`
    - `publicationDate,desc`

Примеры:

    # Первая страница по умолчанию
    curl "http://localhost:8080/products"

    # Вторая страница по 5 элементов, сортировка по цене по убыванию
    curl "http://localhost:8080/products?page=1&size=5&sort=price,desc"

Ответ — стандартный JSON Spring Data:

- `content` — список товаров
- `totalElements`, `totalPages`
- `pageable`, `size`, `number` и т.д.

---

### 4.3. Параллельная фильтрация и сортировка

**GET** `/products/filtered`

Этот эндпоинт использует `parallelStream()` внутри `ProductQueryService`
(фильтрация и сортировка выполняются в параллельном потоке).

Параметры:

- `q` — подстрока для поиска по названию товара (case-insensitive)
- `minPrice` — минимальная цена (`BigDecimal`, например `10` или `99.90`)
- `maxPrice` — максимальная цена
- `sortBy` — поле сортировки:
    - `PRICE`
    - `NAME`
    - `PUBLICATION_DATE`
- `direction` — направление сортировки:
    - `ASC`
    - `DESC`
- `page` — номер страницы (0-based, default `0`)
- `size` — размер страницы (default `20`)

Примеры:

    # Все товары, сортировка по цене по убыванию
    curl "http://localhost:8080/products/filtered?sortBy=PRICE&direction=DESC"

    # Товары, в названии которых есть '101', с ценой от 50 до 100
    curl "http://localhost:8080/products/filtered?q=101&minPrice=50&maxPrice=100"

    # Вторая страница по 3 товара, сортировка по дате публикации по возрастанию
    curl "http://localhost:8080/products/filtered?sortBy=PUBLICATION_DATE&direction=ASC&page=1&size=3"

Ответ — массив DTO:

    [
      {
        "id": 1,
        "name": "101",
        "description": "Demo product parsed from https://example.com/product/101 (external category=electronics, ...)",
        "price": 85.00,
        "publicationDate": "2025-11-19T01:01:55.115",
        "sourceUrl": "https://example.com/product/101"
      }
    ]

---

## 5. Многопоточность и WebClient

- Пул потоков создаётся в `ParsingExecutorConfig` (обычно фиксированный размер пула).
- Планировщик `ParsingScheduler` помечен `@Scheduled` и через заданный интервал:
    - ищет задачи со статусом `NEW`;
    - отправляет их в `ParsingTaskProcessingService`.
- `ParsingTaskProcessingService` выполняет `executorService.submit(...)` для каждой задачи:
    - несколько URL обрабатываются параллельно (несколько потоков).
- В `PriceParsingService` при парсинге URL дополнительно вызывается `ExternalProductInfoClient`
  с помощью `WebClient` — имитация внешнего HTTP-сервиса:
    - полученные данные добавляются к описанию товара.
- В `ProductQueryService` используется `parallelStream()` для параллельной фильтрации и сортировки
  уже сохранённых товаров.

---

## 7. Тесты

Запуск всех тестов:

    mvn test

Тесты покрывают:

- генерацию и обогащение `Product` (`PriceParsingServiceTest`);
- логику обработки задач и смены статусов (`ParsingTaskProcessingServiceTest`);
- REST-контроллер (`PriceParserControllerTest`).

---

## 8. Как подключить реальный парсер (опционально)

Сейчас `PriceParsingService` — демонстрационная заглушка (рандомная цена, описание и т.д.).

Чтобы подключить реальный парсер для одного конкретного магазина:

1. Добавить зависимость на **Jsoup** в `pom.xml`:

        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.18.1</version>
        </dependency>

2. Внутри `PriceParsingService.parseProduct(String url)`:

    - выполнить `Jsoup.connect(url).get();`
    - достать нужные элементы страницы (название, цену, описание);
    - заполнить поля `Product`.