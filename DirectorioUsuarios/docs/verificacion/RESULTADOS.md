# Verificación ejecutada

- APK debug compilado: assembleDebug completado.
- APK de instrumentación compilado: assembleDebugAndroidTest completado.
- testDebugUnitTest: BUILD SUCCESSFUL. 5 pruebas, 0 fallos.
- API real: /users devolvió 10 usuarios; /posts?userId=1 devolvió 10 publicaciones del usuario 1.
- RenderEvidenceTest ejecutó el componente Compose real mediante Robolectric con gráficos nativos y capturó el árbol View en Bitmap. Datos y estados controlados.
- LocalPersistenceTest validó Room al cerrar y reabrir la base y conservar favoritos tras actualizar usuarios.
- No se completó una ejecución de la app en un teléfono físico ni una prueba integral en emulador. La lista manual del README queda como verificación adicional en el equipo del estudiante.

## Resultados JUnit

- edu.utesa.directorio.ApiTest: 3 prueba(s), 0 fallos, 0 errores.
- edu.utesa.directorio.LocalPersistenceTest: 1 prueba(s), 0 fallos, 0 errores.
- edu.utesa.directorio.RenderEvidenceTest: 1 prueba(s), 0 fallos, 0 errores.
