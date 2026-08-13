# LiveTracks

<p align="center">
  <a href="README.md">English</a> · <a href="README.es.md"><strong>Español</strong></a>
</p>

<p align="center">
  <img src="assets/brand/livetracks-mark.png" alt="LiveTracks" width="160">
</p>

<p align="center">
  <strong>Reproducción multipista profesional para shows en vivo, construida para Android.</strong><br>
  Stems sincronizados, click por canción, timeline, consola y un modo de escenario pensado para operar entre temas.
</p>

<p align="center">
  <a href="https://github.com/thomrnowtea/livetracks/actions/workflows/android-ci.yml"><img alt="Android CI" src="https://github.com/thomrnowtea/livetracks/actions/workflows/android-ci.yml/badge.svg"></a>
  <a href="https://github.com/thomrnowtea/livetracks/releases"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/thomrnowtea/livetracks?include_prereleases&label=release"></a>
  <img alt="Android 8+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Estado Alpha" src="https://img.shields.io/badge/status-alpha-D4AD5A">
</p>

> [!IMPORTANT]
> LiveTracks está en **alpha**. Ya puede probarse, pero todavía no debe considerarse validada para un show crítico sin ensayar previamente el teléfono, los archivos, el adaptador y el ruteo exactos que se usarán en vivo.

## La idea

Una sesión de LiveTracks mantiene una jerarquía simple:

```text
Proyecto / show
├── Master del proyecto: volumen, paneo y defaults de metrónomo
└── Playlist
    ├── Canción A: master y metrónomo propios
    │   ├── Drums.wav
    │   ├── Bass.flac
    │   ├── Click.wav  → referencia de click opcional
    │   └── Cues.wav
    └── Canción B: master, tempo y stems independientes
```

Cada canción es un contenedor sincronizado de stems. Todos parten del mismo reloj de audio; cada stem puede desplazarse con precisión de milisegundos, mezclarse y rutearse sin convertirse en un reproductor independiente que pueda derivar del resto.

## Capturas

<p align="center">
  <img src="docs/images/timeline-landscape.png" alt="Timeline de LiveTracks en landscape" width="760">
</p>

<p align="center">
  <img src="docs/images/timeline-portrait.png" alt="Timeline de LiveTracks en portrait" width="320">
  &nbsp;&nbsp;
  <img src="docs/images/stage-mode.png" alt="Modo Escenario de LiveTracks" width="320">
</p>

## Funciones principales

| Área | Capacidades |
|---|---|
| **Proyectos y playlist** | Múltiples shows, canciones reordenables, master de proyecto y master por canción, anterior/siguiente sin autoplay. |
| **Timeline** | Ondas reales, zoom hasta 10 ms, playhead arrastrable, snapping, offsets, split no destructivo, extracción a una canción nueva y Undo/Redo de 50 pasos. |
| **Metrónomo y estructura** | BPM y compás por canción, defaults heredables, grilla musical activable, stem de referencia de click y marcadores Intro/Verso/Estribillo/Puente/Solo. |
| **Mezcla** | Fader, paneo, mute, solo, MAIN/MONITOR y medidores por stem; salida Single Mix o Stereo Split. |
| **Operación en vivo** | Modo Escenario con playlist limpia y controles grandes de anterior, Play/Pause, Stop y siguiente. Pantalla activa y modo exclusivo opcionales. |
| **Archivos** | WAV PCM/float nativo; MP3, AAC/M4A, FLAC y OGG mediante los codecs disponibles en Android; también admite stems vacíos con duración definida. |
| **Distribución** | Releases firmadas, checksum SHA-256 y actualizador integrado con validación de versión, paquete, certificado y archivo. |

La interfaz se adapta a portrait y landscape, está disponible en español e inglés y permite colapsar herramientas o paneles cuando el espacio de trabajo importa más que la edición.

## Flujo de trabajo

1. Crea un proyecto para el show.
2. Agrega y ordena las canciones de la playlist.
3. Importa los stems de cada canción o crea regiones vacías.
4. Alinea entradas en la timeline, agrega marcadores y ajusta el metrónomo.
5. Mezcla niveles, paneo, mute/solo y ruteo MAIN/MONITOR.
6. Valida la salida física con el mismo hardware que usarás en vivo.
7. Entra a Modo Escenario cuando la edición esté terminada.

## Seguridad de audio

El motor C++ usa una única salida Oboe como reloj master. El callback de tiempo real sólo mezcla buffers preasignados y consulta estado atómico: no decodifica, no accede a almacenamiento o red, no llama a JNI/UI y no espera locks.

- Los stems `CLICK` y `CUE` nacen sin envío a MAIN.
- Convertir un stem en referencia de click lo fuerza a MONITOR, silencia su MAIN y suspende el click sintetizado.
- Un cambio de dispositivo durante la reproducción detiene la salida y exige revalidar la ruta.
- Los cues de voz se sintetizan al editar y se reproducen desde audio prerenderizado.
- Bluetooth se considera únicamente una opción de preescucha por su latencia y variabilidad.

Consulta [Audio engine](docs/AUDIO_ENGINE.md) y [Hardware compatibility](docs/HARDWARE_COMPATIBILITY.md) antes de depender de la app en escenario.

## Descargar e instalar

La distribución oficial se realiza mediante [GitHub Releases](https://github.com/thomrnowtea/livetracks/releases):

1. Descarga `LiveTracks.apk` desde la release elegida.
2. Opcionalmente compara el archivo con `LiveTracks.apk.sha256`.
3. Autoriza a Android a instalar desde esa fuente cuando lo solicite.
4. Para futuras versiones usa **Ajustes → Acerca de → Buscar actualizaciones**.

El actualizador no instala silenciosamente. Antes de abrir el instalador de Android valida HTTPS, metadata, versión, package id, SHA-256 y el mismo certificado de firma de la app instalada.

## Estado actual y límites

Los APK oficiales incluyen código nativo para ARM de 32 bits (`armeabi-v7a`), ARM de 64 bits (`arm64-v8a`) y emulador (`x86_64`). El soporte de empaquetado permite instalar en esas familias de CPU, pero no certifica el hardware de audio de un dispositivo físico.

La versión alpha precarga cada archivo en memoria, con un máximo de 16 stems y 512 MB decodificados por archivo. El menor espacio de direcciones y la memoria disponible en muchos equipos de 32 bits hacen especialmente importante probar primero con archivos cortos y conservadores. El streaming por ring buffers, el servicio de reproducción foreground, la validación física de Stereo Split/USB, el autoavance y el Live Mode ampliado siguen pendientes.

La fuente de verdad sobre lo implementado es [Implementation status](docs/STATUS.md). Las afirmaciones de compatibilidad física viven en [Hardware compatibility](docs/HARDWARE_COMPATIBILITY.md); una prueba en emulador no se presenta como validación de escenario.

## Desarrollo

Requisitos:

- Android Studio y JDK 17
- Android SDK 35
- Android NDK `27.0.12077973`
- CMake `3.22.1`

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./gradlew testDebugUnitTest lintDebug verifyDebugApkAbis
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

El proyecto separa `ui`, `domain`, `data`, `audio` y `cpp`; las dependencias apuntan hacia el dominio y el motor nativo nunca depende de UI o persistencia. Los builds verifican `armeabi-v7a`, `arm64-v8a` y `x86_64`.

## Documentación

| Documento | Contenido |
|---|---|
| [Status](docs/STATUS.md) | Funciones terminadas, parciales, pendientes y bloqueadas por hardware. |
| [Architecture](docs/ARCHITECTURE.md) | Capas, flujo de datos y límites entre Kotlin/JNI/C++. |
| [Audio engine](docs/AUDIO_ENGINE.md) | Reloj, mezcla, ruteo y restricciones de tiempo real. |
| [Testing](docs/TESTING.md) | Gates locales, matriz de emulador y secuencia física. |
| [Releases](docs/RELEASES.md) | Versionado, firma, assets y contrato del actualizador. |
| [Changelog](CHANGELOG.md) | Cambios por versión. |

## Contribuir y reportar problemas

Las contribuciones y el feedback son bienvenidos. Antes de abrir un PR, lee [CONTRIBUTING.md](CONTRIBUTING.md) y ejecuta los gates locales. Para bugs, usa el [formulario de reporte](https://github.com/thomrnowtea/livetracks/issues/new?template=bug_report.yml); para propuestas, usa el [formulario de feature](https://github.com/thomrnowtea/livetracks/issues/new?template=feature_request.yml).

No publiques vulnerabilidades, credenciales, archivos de audio privados ni datos de dispositivos en un issue. Sigue [SECURITY.md](SECURITY.md) para reportes sensibles.

## Créditos y licencia

Creado y mantenido por [thomrnowtea](https://github.com/thomrnowtea). Las atribuciones de dependencias y tipografías están en [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

El repositorio es público para permitir su inspección y colaboración, pero actualmente **no concede una licencia general sobre el código fuente**. Hasta que exista un archivo `LICENSE`, se reservan los derechos aplicables; público no significa automáticamente open source.
