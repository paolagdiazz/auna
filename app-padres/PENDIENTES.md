# App de padres · lo que no funcionaba (v0.1) y su estado en v0.2

Revisión pantalla a pantalla. `[x]` = arreglado y comprobado en el emulador.
`[~]` = arreglado, pero solo se puede comprobar del todo en tu móvil (micrófono real o tu clave de Claude).
`[ ]` = pendiente.

## General
- [x] G1 · Las transiciones entre pantallas son un fundido (fade).
- [x] G2 · El pasaporte es fijo: los datos de Marco están escritos en el código y no se pueden añadir, editar ni borrar.
- [x] G3 · El progreso (8/11) es un número fijo, no se calcula.
- [x] G4 · Los perfiles nuevos no tienen nada y no se pueden rellenar.

## Inicio
- [x] I1 · Progreso fijo (ver G3).

## Pasaporte
- [x] P1 · «Escuchar el pasaporte» no hace nada.
- [x] P2 · «Escuchar» de cada sección no hace nada.
- [x] P3 · No se pueden añadir, editar ni borrar cosas dentro de una sección.
- [x] P4 · «Lo más importante» y la etiqueta «No hablo» son fijos.
- [x] P5 · «Otra, con mi nombre» crea siempre «Mi sección» sin preguntar el nombre.
- [x] P6 · Las secciones nuevas no se pueden renombrar ni borrar.
- [x] P7 · «Añadir hablando» no sabe a qué sección añadir.
- [x] P8 · La foto del niño no se puede poner ni cambiar.

## Hablar
- [~] H1 · No graba ni reconoce la voz (el contador es falso). → Usa el reconocimiento de voz de Android en español y sigue escuchando en las pausas. En el emulador no hay micrófono: pruébalo en el móvil.
- [~] H2 · El texto que aparece mientras «escucha» es siempre el mismo. → Muestra en directo lo que reconoce. Pruébalo en el móvil.
- [~] H3 · «Esto he entendido» muestra siempre las mismas 3 frases, digas lo que digas. → Claude ordena lo que cuentas en los apartados del pasaporte. Necesita tu clave en Ajustes.
- [~] H4 · «Editar» no hace nada. → Puedes cambiar el texto y dónde va antes de guardar.
- [~] H5 · «Guardar en el pasaporte» no guarda nada. → Guarda lo que dejes marcado en su apartado del pasaporte.
- [~] H6 · La pregunta de seguimiento es siempre la misma. → La pregunta la propone Claude según lo que falta.
- [x] H7 · Lo que escribes en «Prefiero escribir» se ignora.
- [x] H8 · El tema elegido no cambia nada.

## Comprueba las respuestas
- [~] C1 · Solo hay 3 preguntas con respuestas fijas. → Claude responde cualquier pregunta usando solo lo que dice el pasaporte. Necesita tu clave.
- [~] C2 · Escribir otra pregunta y «Enviar» no hace nada; el micro tampoco. → Se puede escribir o dictar la pregunta.
- [x] C3 · «Está bien» / «Corregir» no guardan nada; «Corregir» no sabe qué corregir.

## Accesos
- [x] A1 · «Invitar» no hace nada.
- [x] A2 · «Cambiar» no hace nada.
- [ ] A3 · La solicitud de Pedro es de ejemplo y no llega de nadie. → Pendiente del servidor (decidido: por ahora todo es local). Aceptar o rechazar sí funciona.
- [x] A4 · «Quitar acceso» no se puede deshacer ni pide confirmación.

## Propuestas
- [x] PR1 · «Editar» no hace nada.
- [ ] PR2 · Las propuestas son de ejemplo y no llegan de nadie. → Pendiente del servidor (decidido: por ahora todo es local). Añadir, editar y descartar sí funcionan.

## Mi tarjeta QR
- [x] Q1 · El QR es un dibujo, no un código que se pueda escanear.
- [x] Q2 · «Imprimir» no hace nada.
- [x] Q3 · «Descargar» no hace nada.
- [x] Q4 · «He perdido la tarjeta» no hace nada.
- [x] Q5 · Las casillas de «Qué se muestra» no cambian lo que se ve.
- [x] Q6 · La vista de quien escanea tiene datos fijos de Marco (edad, epilepsia…).
- [x] Q7 · «Llamar a mamá / papá» y los botones de llamar de tutores no hacen nada (no hay teléfonos).
- [x] Q8 · «Enviar mi ubicación» es falso. → Quitado: el QR ya no abre una web; lleva la ficha de emergencia. Volverá con la web app.
- [x] Q9 · El inicio de sesión de la vista previa es falso. → Quitado por lo mismo: no hay inicio de sesión sin la web app.

## Ajustes
- [x] AJ1 · «Leer en voz alta» se guarda pero no hace nada.
- [x] AJ2 · «Avisos por WhatsApp» se guarda pero no hace nada. → Quitado: sin servidor no se pueden mandar avisos. Vuelve con la web app.

## Mis perfiles
- [x] PF1 · No se puede poner foto.
- [x] PF2 · No se puede cambiar el nombre ni la edad.
- [x] PF3 · «Su pasaporte / Su QR / Sus accesos» no se pueden tocar.
