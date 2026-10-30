# требования
* Регистрация не выполняет вход
* Подтверждение пароля проверяется на фронтенде и не отправляется
* Ошибки полей возвращаются в `fields`

# пользовательские сценарии
## 1 Успешная регистрация
`POST /api/auth/sign-up`

запрос:
```json
{
  "email": "user@example.com",
  "password": "secret-password"
}
```

ответ: `201 Created`
```json
{
  "id": 1,
  "email": "user@example.com"
}
```

## 2 Некорректные поля
`POST /api/auth/sign-up`

запрос:
```json
{
  "email": "invalid",
  "password": ""
}
```

ответ: `400 Bad Request`
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Проверьте введённые данные",
  "fields": {
    "email": "Некорректный адрес электронной почты",
    "password": "Пароль не соответствует требованиям"
  }
}
```

## 3 Адрес уже зарегистрирован
`POST /api/auth/sign-up`

запрос:
```json
{
  "email": "user@example.com",
  "password": "secret-password"
}
```

ответ: `409 Conflict`
```json
{
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "Этот адрес уже зарегистрирован",
  "fields": {
    "email": "Этот адрес уже занят"
  }
}
```
