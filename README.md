# Simple Log

App Android mínima (Android 8.0 o posterior) que guarda localmente cuándo una aplicación pasa a primer plano:

```text
16/10/2026 13:09:25 - Spotify
16/10/2026 13:12:08 - WhatsApp
16/10/2026 13:15:42 - Spotify
```

## Instalación

1. En GitHub, abre **Actions → Build Android APK → última ejecución correcta**.
2. Descarga el artefacto **simple-log-apk** y extrae `app-debug.apk` del ZIP.
3. Instala el APK en tu Android (autoriza la instalación desde el navegador o gestor de archivos si Android lo pide).
4. Abre Simple Log, pulsa **Dar acceso a datos de uso** y habilita Simple Log en Ajustes.
5. Vuelve a la app. El historial se actualiza al abrirla o pulsar **Actualizar**.

**Exportar TXT** guarda todas las entradas con el selector de documentos de Android. No necesita permiso de almacenamiento. La pantalla muestra las últimas 1.000; la base de datos conserva todas hasta borrar los datos o desinstalar la app. El historial se presenta en la zona horaria actual del móvil.

## Colores del historial

Toca una entrada para elegir su fondo. Se aplica a todas las entradas con el mismo nombre de aplicación, antiguas y futuras, y se guarda localmente. Puedes elegir entre ocho opciones, quitar el color o restablecer el predeterminado. Administrador del sistema aparece en gris por defecto. La exportación TXT conserva el texto sin colores. Instala la nueva versión sobre la anterior para conservar el historial.

## Qué registra

- Abrir una app o volver a ella desde otra aplicación o desde el escritorio.
- Volver a una app después de apagar y encender la pantalla.
- Cambiar de pantalla dentro de una misma app no crea entradas.
- Se omiten Simple Log, el escritorio y la interfaz del sistema, pero se tienen en cuenta sus transiciones para detectar el regreso a otra app.
- Si Android no permite resolver el nombre de una app (por ejemplo, una app desinstalada), aparece su identificador de paquete.

Se usan los eventos `ACTIVITY_RESUMED` del sistema, no una observación del contenido de pantalla. Ventanas múltiples, algunas pantallas del sistema y modificaciones de fabricantes pueden producir comportamientos diferentes. La primera lectura intenta recuperar las últimas 24 horas disponibles; no puede inventar eventos que Android ya haya eliminado.

## Privacidad y segundo plano

- Único permiso solicitado: **Acceso a datos de uso** (`PACKAGE_USAGE_STATS`).
- Sin permiso de Internet, anuncios, analítica, SDK externos, accesibilidad, contactos, ubicación ni permiso de almacenamiento.
- Datos en SQLite privada, copia de seguridad desactivada.
- Android programa una recogida aproximadamente cada 15 minutos, pero puede retrasarla por ahorro de batería. No hay servicio permanente ni notificación.
- El trabajo periódico se programa al abrir la app. **Tras reiniciar el móvil o forzar su detención, vuelve a abrir Simple Log.** No se solicita permiso para arrancar al encender el móvil.
- Android conserva eventos solo durante unos días: abre la app cada pocos días. Un intervalo largo sin recogida puede dejar huecos irrecuperables. No se garantiza una auditoría exhaustiva.

## Compilación

Java 17, Android SDK 35, Android Gradle Plugin 8.9.2 y Gradle 8.11.1. Sin dependencias de ejecución de terceros.

Con Gradle 8.11.1 instalado y el SDK configurado (`ANDROID_HOME` o `local.properties`):

```sh
gradle testDebugUnitTest lintDebug assembleDebug
```

GitHub Actions ejecuta las pruebas de transiciones, lint y compilación en cada push a `main`, pull request y ejecución manual. El APK se publica como artefacto con **`retention-days: 1`**, y la caché de Gradle en GitHub está desactivada. Un día de retención reduce el almacenamiento acumulado; el artefacto ocupa almacenamiento durante ese día y las compilaciones siguen consumiendo minutos si el plan los contabiliza.

La clave de desarrollo incluida es **pública y solo para uso personal/pruebas**. Mantiene la firma de los APK entre compilaciones para poder actualizar sin desinstalar ni perder el historial. No se debe usar para publicar en una tienda ni como clave de producción.
