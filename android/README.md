# ABClient Android Build Configuration

## Настройка подписи релиза

Для сборки подписанного релиза необходимо настроить файл `gradle.properties` с параметрами подписи.

### Шаги настройки:

1. **Скопируйте пример файла:**
   ```bash
   cp gradle.properties.example gradle.properties
   ```

2. **Отредактируйте gradle.properties:**
   Раскомментируйте и заполните следующие параметры:
   ```properties
   RELEASE_STORE_FILE=your-keystore-file.jks
   RELEASE_KEY_ALIAS=your-key-alias
   RELEASE_STORE_PASSWORD=your-store-password
   RELEASE_KEY_PASSWORD=your-key-password
   ```

3. **Поместите ваш файл ключа (.jks) в папку app/:**
   ```
   android/app/your-keystore-file.jks
   ```

### Сборка релиза:

```bash
./gradlew assembleRelease
```

### Безопасность:

- Файл `gradle.properties` **НЕ ДОБАВЛЯЕТСЯ** в git (находится в .gitignore)
- Файлы ключей (*.jks) **НЕ ДОБАВЛЯЮТСЯ** в git
- Используйте `gradle.properties.example` как шаблон для новых разработчиков

### Файлы, исключенные из git:

- `gradle.properties` - содержит пароли подписи
- `*.jks`, `*.keystore` - файлы ключей подписи
- `build/` - директории сборки

### Примечания:

- При первой настройке скопируйте все настройки из `gradle.properties.example`
- Убедитесь, что путь к JDK указан правильно в `org.gradle.java.home`
- Для публичных репозиториев **НИКОГДА** не коммитьте файлы с паролями и ключами