# Security policy

LiveTracks controla reproducción de audio, solicita acceso a archivos elegidos por el usuario y puede entregar actualizaciones al instalador de Android. Los reportes responsables son importantes, especialmente si involucran la cadena de releases, validación de APK, acceso a documentos, ruteo de click/cues o interrupciones durante una actuación.

## Versiones soportadas

LiveTracks todavía está en alpha. Sólo la pre-release más reciente recibe correcciones de seguridad; las versiones anteriores se mantienen únicamente para comprobar migraciones y actualizaciones.

| Versión | Soporte |
|---|---|
| Última pre-release | Sí |
| Pre-releases anteriores | No |
| Builds debug o forks | No |

## Reportar una vulnerabilidad

Usa **Security → Report a vulnerability** en GitHub para enviar un reporte privado. No abras un issue público y no adjuntes credenciales, keystores, audios privados, rutas locales ni información identificable de un dispositivo.

Incluye, cuando sea posible:

- versión y origen del APK;
- versión de Android;
- componente afectado y pasos mínimos de reproducción;
- impacto esperado y observado;
- logs sanitizados, sin datos privados;
- evidencia de manipulación si el problema afecta una actualización o firma.

El proyecto acusará recibo cuando el reporte sea revisado. Los tiempos de corrección y divulgación dependerán del impacto y de la posibilidad de reproducir el problema.

## Alcance

Son especialmente relevantes:

- aceptación de un APK con package id, checksum o certificado incorrectos;
- bypass del consentimiento del instalador de Android;
- exposición de documentos no seleccionados por el usuario;
- click o cue audible por MAIN pese a una configuración segura;
- operaciones de red, almacenamiento, JNI o bloqueo dentro del callback de audio;
- pérdida silenciosa o corrupción de proyectos durante una migración.

La latencia propia de Bluetooth, codecs ausentes en un fabricante y fallas de hardware no validado no son vulnerabilidades, aunque pueden reportarse como bugs sin datos sensibles.
