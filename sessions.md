# требования
* JWT содержит идентификатор пользователя и срок действия
* При входе сервер устанавливает cookie `access_token` с `HttpOnly`, `SameSite=Lax`, `Path=/` и `Secure` в production
* JWT не возвращается в JSON
* На защищённых запросах сервер проверяет подпись и срок действия JWT
* При выходе сервер очищает cookie, скопированный JWT остаётся действительным до истечения срока

# пользовательские сценарии
## 1 Успешный вход
`POST /api/auth/sign-in`

запрос:
```json
{
  "email": "user@example.com",
  "password": "secret-password"
}
```

ответ: `200 OK`, cookie `access_token`
```json
{
  "id": 1,
  "email": "user@example.com"
}
```

## 2 Неверная почта или пароль
`POST /api/auth/sign-in`

запрос:
```json
{
  "email": "user@example.com",
  "password": "wrong-password"
}
```

ответ: `401 Unauthorized`
```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "неверная почта или пароль"
}
```

## 3 Некорректные поля входа
`POST /api/auth/sign-in`

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
  "message": "проверьте введённые данные",
  "fields": {
    "email": "некорректный адрес электронной почты",
    "password": "введите пароль"
  }
}
```

## 4 Получение текущего пользователя
`GET /api/auth/me`

запрос: без тела, cookie `access_token`

ответ: `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com"
}
```

## 5 Отсутствующая или недействительная авторизация
`GET /api/auth/me`

запрос: без тела, cookie отсутствует или содержит недействительный JWT

ответ: `401 Unauthorized`
```json
{
  "code": "UNAUTHENTICATED",
  "message": "необходимо войти"
}
```

## 6 Выход
`POST /api/auth/sign-out`

запрос: без тела, cookie `access_token` при наличии

ответ: `200 OK`, cookie очищена. Повторный выход также успешен
```json
{
  "message": "выход выполнен"
}
```
