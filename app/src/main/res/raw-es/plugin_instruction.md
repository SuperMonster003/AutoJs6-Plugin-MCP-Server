MCP Server permite que los clientes de IA de su ordenador, como Claude Code y Cursor, manejen este teléfono mediante el [Model Context Protocol](https://modelcontextprotocol.io). Una vez conectado por USB o por la red local, un cliente puede usar AutoJs6 para ejecutar scripts, inspeccionar y manejar la pantalla, tomar capturas de pantalla y leer y escribir archivos del directorio de trabajo. Cada cliente nuevo debe confirmarse en el teléfono la primera vez que llama a una herramienta.

### Primeros pasos

1. Encienda el interruptor "Servidor MCP" en el cajón de AutoJs6 y active el plugin cuando se le solicite la primera vez.
2. Mantenga pulsado el elemento o abra los ajustes del plugin en el "Centro de plugins", copie la configuración de su cliente desde "Configuración del cliente" y añádala al cliente MCP de su ordenador.
3. Conecte el teléfono al ordenador por USB y ejecute en el ordenador el comando adb forward que aparece en "Conexión" (toque "Copiar comando adb forward" para copiarlo).
4. Cuando el cliente llame a una herramienta por primera vez, toque "Permitir" en el teléfono para confirmar la vinculación.

### Funciones principales

- **Scripts y pantalla**: los clientes pueden ejecutar scripts en AutoJs6 y leer la salida de la consola, exportar el árbol de nodos de accesibilidad, buscar controles y tocarlos, escribir o desplazarse, y tomar capturas de pantalla.
- **Archivos y dispositivo**: leer y escribir archivos del directorio de trabajo de AutoJs6, abrir aplicaciones y leer o escribir el portapapeles. Con "Permitir leer el almacenamiento compartido" activado, los clientes también pueden leer, sin modificarlas, ubicaciones como la galería y la carpeta de descargas.
- **Permisos de herramientas**: active o desactive los grupos de herramientas disponibles para los clientes; un grupo desactivado no aparece en la lista de herramientas del cliente. "Eliminación de archivos", "Gestos por coordenadas" y "Comandos Shell" están desactivados de forma predeterminada.
- **Herramientas de plugins**: con plugins como OCR, Barcode y OpenCV instalados, los clientes pueden reconocer texto y códigos de barras y buscar imágenes y colores. Las herramientas que declaran otros plugins se activan en "Herramientas de plugins".
- **Scripts registrados**: los scripts registrados con un comentario `@agent` se muestran a los clientes, y el modelo puede ejecutarlos con los parámetros registrados; para los scripts que requieren confirmación, el cliente le pide su consentimiento antes de ejecutarlos.
- **Token y vinculación**: los clientes se conectan con un token de acceso, del que la página de ajustes solo muestra los últimos 4 caracteres. Revoque vinculaciones en "Clientes vinculados"; tras "Renovar token", el token anterior deja de funcionar de inmediato.
- **Conexiones de red local**: con "Permitir conexiones de red local" activado, los ordenadores de la misma red pueden conectarse sin USB; el token y la confirmación de vinculación siguen siendo necesarios.
- **Detener automáticamente en reposo**: detiene el servidor cuando no llega ninguna solicitud en el tiempo elegido; desactivado de forma predeterminada.

### Requisitos

- Android 7.0 o posterior y AutoJs6 6.8.0 o posterior.
- Un cliente MCP en el ordenador; la conexión USB también necesita adb. Los clientes que solo admiten stdio, como Claude Desktop, necesitan el [puente](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge) instalado aparte.
- Las operaciones en pantalla y las capturas de pantalla necesitan el servicio de accesibilidad de AutoJs6; en algunos dispositivos, las capturas también requieren autorizarlas en el teléfono.
- El permiso de notificaciones permite confirmar las solicitudes de vinculación desde cualquier pantalla; en Android 17 y posteriores también hay que permitir que este plugin acceda a dispositivos cercanos (para la red local).
- Las herramientas de plugins, como el reconocimiento de texto y de códigos de barras, necesitan el plugin correspondiente instalado y activado en AutoJs6.

### Preguntas frecuentes

**Motivos por los que el cliente indica 401 Unauthorized**

El token falta, se escribió mal o se renovó. Vuelva a copiar la configuración desde la página de ajustes; tras renovar el token, cada cliente necesita el nuevo.

**Qué hacer cuando se rechaza la conexión**

Compruebe que el interruptor "Servidor MCP" del cajón esté encendido. Volver a conectar el dispositivo o reiniciar adb borra el reenvío de puertos; en ese caso, ejecute de nuevo el comando adb forward en el ordenador.

**Motivos por los que el servidor se detiene con la pantalla apagada**

Cuando el sistema restringe el uso de batería de esta aplicación, puede detener el servidor con la pantalla apagada. La página de ajustes muestra entonces un aviso; toque "Ajustes de batería" y elija "Sin restricciones" para MCP Server.

**Motivos por los que una herramienta no aparece en el cliente**

Su grupo está desactivado en "Permisos de herramientas", o el plugin que necesita no está instalado o activado en AutoJs6. La lista de herramientas del cliente se actualiza sola una vez activado.

La configuración de cada cliente, la lista de herramientas y las notas de seguridad están en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server).
