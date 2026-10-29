# Flashly

App for studying with flashcards and decks.

Aplicación Android (Kotlin + Jetpack Compose) de tarjetas de aprendizaje cuyo backend completo está en **Supabase**: Auth, PostgreSQL con RLS, funciones RPC, Storage y Realtime. Proyecto del curso Desarrollo de Aplicaciones Móviles, Universidad de Nariño.

## Funcionalidades

- Registro e inicio de sesión, perfil con avatares predefinidos y cambio de contraseña.
- Mazos con preguntas de selección múltiple (opciones sin límite) y tarjetas de definición que giran.
- Biblioteca, explorar mazos públicos con búsqueda y categorías, guardar y compartir.
- Cuatro modos de juego: Práctica, Contrarreloj, Repaso inteligente (repetición espaciada) y Reto diario.
- Historial con gráficas y ranking semanal en tiempo real.

Las respuestas, el XP, los combos, las vidas y el tiempo se validan en el servidor (funciones SQL), no en el teléfono.

## Cómo ejecutarlo

1. Abrir la carpeta en Android Studio y esperar la sincronización de Gradle.
2. En `local.properties` (no se sube al repositorio) agregar las dos líneas del proyecto de Supabase; pídelas al equipo:
   ```
   SUPABASE_URL=https://<proyecto>.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
   ```
   Solo se usa la clave publicable. Nunca poner aquí la clave secreta ni la service_role.
3. Ejecutar en un emulador o teléfono con Android 8.0 o superior.

## Estructura

| Ruta | Contenido |
| --- | --- |
| `supabase/schema.sql` | Tablas, políticas RLS, disparadores y funciones del backend (las secciones 1-4 son la versión 1 y se retiran en la sección 8) |
| `app/src/main/java/com/udenar/flashly/data/` | Cliente de Supabase y repositorios: cuenta, mazos y estudio |
| `app/src/main/java/com/udenar/flashly/ui/` | Pantallas: auth, main, mazos, editor, estudio, historial, perfil y componentes |
| `app/src/test/` | Pruebas unitarias |
