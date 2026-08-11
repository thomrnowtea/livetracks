# LiveTracks

![Marca de LiveTracks](assets/brand/livetracks-mark.png)

LiveTracks es una consola multipista para Android pensada para preparar y reproducir shows en vivo. Organiza cada show como un proyecto, cada canción como una pista master y cada archivo de audio como un stem sincronizado. La estabilidad, un único reloj de audio y el ruteo seguro del click tienen prioridad sobre la cantidad de funciones.

## Qué ofrece

- Proyectos independientes con volumen, paneo, playlist y plantilla de metrónomo propios.
- Playlist ordenable de canciones; cada canción conserva sus stems, mezcla y configuración individual de metrónomo.
- Importación mediante el selector de documentos de Android: WAV se entrega directamente al motor; MP3, AAC/M4A, FLAC y OGG compatibles con el decoder del dispositivo se convierten a PCM fuera del hilo de audio.
- Stems vacíos con nombre y duración configurable para reservar regiones, pausas o futuras entradas sin elegir un archivo.
- Timeline multipista con ondas calculadas desde el audio real, playhead global arrastrable, timecode con milisegundos y zoom desde vista general hasta una grilla de 10 ms.
- Movimiento preciso de clips con snapping al playhead y a los bordes de otros stems.
- Split no destructivo en el playhead: crea un stem nuevo que referencia la segunda parte del mismo archivo sin modificar el WAV original.
- Historial de hasta 50 ediciones para deshacer y rehacer importaciones, altas vacías, movimientos, splits y eliminaciones de stems.
- Mix Console horizontal con fader, paneo, mute, solo, envíos MAIN/MONITOR y medidores por stem.
- Master de dos etapas: salida general del proyecto y salida de la canción seleccionada.
- Metrónomo por canción, con valores heredables del proyecto. No existe un metrónomo global de reproducción.
- Salida `Single Mix` o `Stereo Split`, donde MAIN y MONITOR pueden separarse en los canales físicos izquierdo y derecho.
- Interfaz adaptativa para portrait y landscape, disponible en español e inglés.
- Ajustes generales, defaults de importación, información de hardware y créditos dentro de la aplicación.

## Flujo de trabajo

1. Crea un proyecto para el show.
2. Agrega las canciones en Playlist y ordénalas según el set.
3. Abre una canción, importa sus archivos o crea stems vacíos y alinea cada región en Timeline.
4. Usa el playhead, el zoom y Split para corregir entradas con precisión de milisegundos.
5. Ajusta niveles, paneo, mute/solo y envíos desde Mix Console.
6. Configura el master del proyecto, el master de cada canción y su metrónomo.
7. Valida la salida física exacta que vas a utilizar antes de tocar en vivo.

## Audio y seguridad

El motor C++ usa una única salida Oboe como reloj master. Todos los stems se mezclan para el mismo rango absoluto de frames; no se crean reproductores independientes que puedan derivar entre sí. El callback de audio sólo realiza DSP acotado sobre buffers preasignados.

Los stems `CLICK` y `CUE` nacen con su envío a MAIN desactivado. Un cambio de dispositivo físico durante la reproducción detiene la salida y obliga a revalidar la ruta. Bluetooth queda limitado a preescucha por su latencia y comportamiento variable.

Formatos nativos: RIFF WAV mono o estéreo, PCM de 8/16/24/32 bits o float32. Los formatos comprimidos aceptados por Android se decodifican previamente a PCM mono/estéreo; la disponibilidad exacta depende de los codecs del dispositivo. La versión actual precarga cada archivo en memoria, con un máximo de 16 stems y 512 MB decodificados por archivo; el streaming mediante ring buffers sigue pendiente antes de considerar el producto listo para shows largos.

## Instalación

Mientras LiveTracks no se distribuya por Play Store, las instalaciones oficiales dependen de [GitHub Releases](https://github.com/thomrnowtea/livetracks/releases). Descarga `LiveTracks.apk` y compara su checksum con `LiveTracks.apk.sha256`. Android puede pedir autorización para instalar desde el navegador o administrador de archivos utilizado.

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
