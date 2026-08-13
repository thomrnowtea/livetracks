# LiveTracks

![Marca de LiveTracks](assets/brand/livetracks-mark.png)

LiveTracks es una consola multipista para Android pensada para preparar y reproducir shows en vivo. Organiza cada show como un proyecto, cada canción como una pista master y cada archivo de audio como un stem sincronizado. La estabilidad, un único reloj de audio y el ruteo seguro del click tienen prioridad sobre la cantidad de funciones.

## Qué ofrece

- Proyectos independientes con volumen, paneo, playlist y plantilla de metrónomo propios.
- Playlist ordenable de canciones; cada canción conserva sus stems, mezcla y configuración individual de metrónomo.
- Playlist con cabecera de preparación colapsable y un Modo Escenario dedicado: lista limpia, canción armada, siguiente canción visible y controles extragrandes de anterior, Play/Pause, Stop y siguiente.
- Importación mediante el selector de documentos de Android: WAV se entrega directamente al motor; MP3, AAC/M4A, FLAC y OGG compatibles con el decoder del dispositivo se convierten a PCM fuera del hilo de audio.
- Stems vacíos con nombre y duración configurable para reservar regiones, pausas o futuras entradas sin elegir un archivo.
- Timeline multipista con ondas calculadas desde el audio real, playhead global arrastrable, timecode con milisegundos y zoom desde vista general hasta una grilla de 10 ms.
- Grilla musical por canción con compases amarillos, pulsos claramente diferenciados y visibilidad activable desde las herramientas de Timeline.
- Modo de timeline limpia: Timeline/Mixer vive en la cabecera global; las herramientas y el panel descriptivo de stems se minimizan al tacto y conservan su estado al rotar el dispositivo.
- Barra de edición compacta: agregar, deshacer, rehacer y dividir permanecen visibles; zoom, marcadores, extracción y eliminación viven en un menú contextual accesible sin scroll horizontal.
- Movimiento preciso de clips con snapping al playhead y a los bordes de otros stems.
- Split no destructivo en el playhead: crea un stem nuevo que referencia la segunda parte del mismo archivo sin modificar el WAV original.
- Extracción de cualquier clip —incluido el segundo fragmento de un split— a una pista master independiente, insertada junto a la original y con su propio metrónomo configurable.
- Regla musical por pista con golpes y comienzos de compás derivados del BPM, numerador y denominador efectivos; los clips pueden hacer snap a golpes, playhead, marcadores y bordes.
- Marcadores de secciones como intro, verso, estribillo, puente o solo, visibles sobre todos los stems, arrastrables con precisión de milisegundos y con cue de voz anticipado opcional.
- Historial de hasta 50 ediciones para deshacer y rehacer importaciones, altas vacías, movimientos, splits y eliminaciones de stems.
- Transporte de show con anterior/siguiente: cambiar de canción siempre detiene la salida y arma la nueva pista sin reproducirla automáticamente.
- Transporte jerárquico con Play dominante, controles sobre una barra de progreso de ancho completo y acceso desplegable a Stop, Timeline, Mix Console, Master y Panic.
- Mix Console horizontal con fader, paneo, mute, solo, envíos MAIN/MONITOR y medidores por stem.
- Master de dos etapas: salida general del proyecto y salida de la canción seleccionada.
- Master organizado por intención en cuatro vistas iconográficas: salida del show, salida de canción, click/tempo y ruteo.
- Metrónomo por canción, con valores heredables del proyecto. No existe un metrónomo global de reproducción.
- Cualquier stem seleccionado puede convertirse en la referencia audible de click: queda identificado como `REF`, reemplaza el click nativo y se protege como salida exclusiva de MONITOR. BPM y compás siguen gobernando grilla, snapping y cues.
- Salida `Single Mix` o `Stereo Split`, donde MAIN y MONITOR pueden separarse en los canales físicos izquierdo y derecho.
- Interfaz adaptativa para portrait y landscape, disponible en español e inglés.
- Ajustes generales, defaults de importación, información de hardware y créditos dentro de la aplicación.
- Modo de escenario configurable: pantalla activa y modo exclusivo con foco de audio y supresión temporal de interrupciones, sujeto al permiso explícito de No molestar de Android.
- Actualizador integrado para releases oficiales: consulta automática o manual, canal estable/pre-release configurable, descarga reanudable, validación SHA-256, identidad de paquete y certificado antes de abrir el instalador de Android.

## Flujo de trabajo

1. Crea un proyecto para el show.
2. Agrega las canciones en Playlist y ordénalas según el set.
3. Abre una canción, importa sus archivos o crea stems vacíos y alinea cada región en Timeline.
4. Usa el playhead, el zoom y Split para corregir entradas; si un fragmento necesita otro tempo, extráelo a una pista master nueva.
5. Agrega marcadores de estructura y define si la voz debe anunciarlos por MONITOR con anticipación en golpes.
6. Ajusta niveles, paneo, mute/solo y envíos desde Mix Console.
7. Configura el master del proyecto, el master de cada canción y su metrónomo.
8. Valida la salida física exacta que vas a utilizar antes de tocar en vivo.
9. Entra a Modo Escenario desde Playlist para operar el show sin controles de edición; seleccionar o saltar arma la canción y Play sigue siendo siempre explícito.

## Audio y seguridad

El motor C++ usa una única salida Oboe como reloj master. Todos los stems se mezclan para el mismo rango absoluto de frames; no se crean reproductores independientes que puedan derivar entre sí. El callback de audio sólo realiza DSP acotado sobre buffers preasignados.

Los stems `CLICK` y `CUE` nacen con su envío a MAIN desactivado. Un cambio de dispositivo físico durante la reproducción detiene la salida y obliga a revalidar la ruta. Bluetooth queda limitado a preescucha por su latencia y comportamiento variable.

Los cues hablados se sintetizan y validan al editar, nunca durante el callback de audio. En reproducción se cargan como fuentes pre-renderizadas, solo-safe y con envío exclusivo a MONITOR; para oírlos separados de FOH se debe validar Stereo Split y el cableado físico. El dispositivo necesita una voz TTS instalada en el idioma elegido. El modo exclusivo restaura la política de interrupciones anterior al pausar, detener, entrar en pánico o cerrar normalmente la app.

Formatos nativos: RIFF WAV mono o estéreo, PCM de 8/16/24/32 bits o float32. Los formatos comprimidos aceptados por Android se decodifican previamente a PCM mono/estéreo; la disponibilidad exacta depende de los codecs del dispositivo. La versión actual precarga cada archivo en memoria, con un máximo de 16 stems y 512 MB decodificados por archivo; el streaming mediante ring buffers sigue pendiente antes de considerar el producto listo para shows largos.

## Instalación

Mientras LiveTracks no se distribuya por Play Store, las instalaciones oficiales dependen de [GitHub Releases](https://github.com/thomrnowtea/livetracks/releases). La app puede buscar, descargar, verificar y entregar una nueva versión al instalador de Android desde Ajustes > Acerca de. La primera vez, Android puede pedir permiso para usar LiveTracks como fuente de instalación; la confirmación final nunca es silenciosa. También se puede descargar `LiveTracks.apk` manualmente y comparar su checksum con `LiveTracks.apk.sha256`.

No instales como release de escenario un APK cuyo hardware y archivos reales no hayan sido probados. El estado verificable y las limitaciones vigentes están en [docs/STATUS.md](docs/STATUS.md) y [docs/HARDWARE_COMPATIBILITY.md](docs/HARDWARE_COMPATIBILITY.md).

## Desarrollo

Requisitos:

- Android Studio con JDK 17
- Android SDK 35
- Android NDK 27.0.12077973
- CMake 3.22.1

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./gradlew testDebugUnitTest lintDebug assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Los pushes y pull requests ejecutan tests, lint y builds nativos para `arm64-v8a` y `x86_64`. Los tags semánticos (`v0.x.y`) generan releases firmadas, checksum SHA-256 y metadata de actualización; los tags con sufijo, como `v0.2.0-rc1`, producen pre-releases. Consulta [CONTRIBUTING.md](CONTRIBUTING.md), [docs/TESTING.md](docs/TESTING.md) y [docs/RELEASES.md](docs/RELEASES.md).

## Créditos

Creado por [thomrnowtea](https://github.com/thomrnowtea/livetracks).

Las atribuciones de terceros están en [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Aún no se seleccionó una licencia general para el código fuente del proyecto.
