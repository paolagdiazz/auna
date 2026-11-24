package com.auna.padres.data

/** Datos de ejemplo del diseño. Marco es el perfil con el pasaporte relleno. */
object Sample {
    const val MARCO = "marco"

    val kids = listOf(
        Kid(MARCO, "Marco", "9 años", 8, photo = "marco"),
        Kid("lucas", "Lucas", "7 años", 3, photo = "lucas"),
    )

    val highlights = listOf(
        "utensils" to "Como triturado",
        "mute" to "Los ruidos fuertes me alteran",
        "smile" to "Si me río, no siempre estoy contento",
    )

    /** Secciones fijas del pasaporte, en el orden de la cuadrícula. */
    val baseSections = listOf("sobre", "idioma", "calma", "gusta", "comida", "salud")

    val sectionTitles = mapOf(
        "sobre" to "Sobre mí",
        "idioma" to "Mi idioma",
        "calma" to "Qué me calma",
        "gusta" to "Me gusta",
        "comida" to "Comida",
        "salud" to "Salud y cuidados",
    )

    /** Contenido del pasaporte de Marco. */
    val marcoSections = mapOf(
        "sobre" to PassportSection(
            "Sobre mí",
            listOf(
                Group("Yo", listOf("Me llamo Marco", "9 años", "No hablo")),
                Group("Mi gente", listOf("Mamá · Ana", "Papá · Javier", "Mi hermana Lucía")),
            ),
        ),
        "idioma" to PassportSection("Mi idioma"),
        "calma" to PassportSection(
            "Qué me calma",
            listOf(
                Group("Me calma", listOf("El agua", "Música de pelis", "Hablarme bajito", "Mi mantita azul")),
                Group("Me altera", listOf("Ruidos fuertes", "Mucha gente", "Cogerme de golpe", "Esperar mucho"), warm = true),
            ),
        ),
        "gusta" to PassportSection(
            "Me gusta",
            listOf(Group("Me encanta", listOf("El agua", "La música", "Los columpios", "Mi hermana Lucía", "Pelotas con ruido"))),
        ),
        "comida" to PassportSection(
            "Comida",
            listOf(
                Group("Cuidado", listOf("Todo triturado", "Me atraganto con trozos"), warm = true),
                Group("Cómo como", listOf("Vaso con tapa", "Necesito ayuda", "Sin alergias")),
            ),
        ),
        "salud" to PassportSection(
            "Salud y cuidados",
            listOf(
                Group("Salud", listOf("Síndrome de Angelman", "Epilepsia")),
                Group("Si tengo una crisis", listOf("Llama al 112", "Ponme de lado", "No me sujetes", "Nada en la boca", "Mira cuánto dura"), warm = true),
            ),
            onlyFamily = true,
            pending = "Faltan: transferencias, descanso, higiene",
        ),
    )

    val signals = listOf(
        Signal("Me río mucho", "No siempre estoy contento", "Fíjate en lo demás que hago."),
        Signal("Me tiro de la oreja y lloro", "Me duele algo", "Pictos del cuerpo. Avisa a mamá."),
        Signal("Me tapo los oídos y me río fuerte", "Hay mucho ruido", "Sitio tranquilo y mi música."),
        Signal("Te llevo a la nevera", "Tengo hambre o sed", "Pictos de comida y bebida."),
        Signal("Doy palmas mirando la puerta", "Quiero salir", "Si se puede, sal conmigo."),
        Signal("Me muerdo la mano", "Estoy nervioso", "Voz bajita y mi juguete de agua."),
        Signal("Me froto los ojos y me tumbo", "Tengo sueño", "Déjame descansar tranquilo."),
    )

    val extraSectionOptions = listOf("Colegio", "Sueño", "Higiene", "Rutinas")

    val topics = listOf(
        Triple("comunica", "Cómo se comunica", "¿Qué hace {n} cuando le duele algo?"),
        Triple("comida", "Comida", "¿Cómo come {n}? ¿Necesita ayuda?"),
        Triple("calma", "Qué le calma", "¿Qué le ayuda a tranquilizarse?"),
        Triple("salud", "Salud", "¿Qué debe saber alguien sobre su salud?"),
    )

    val reviewItems = listOf(
        Triple("a", "Mi idioma", "Me tiro de la oreja y lloro → me duele algo."),
        Triple("b", "Mi idioma", "Qué hacer: enséñame los pictos del cuerpo y avisa a mamá."),
        Triple("c", "Qué me calma", "Me ayuda que me hablen bajito."),
    )

    val starters = listOf("Cuando se tira de…", "Le calma…", "No le gusta…")

    val qa = listOf(
        QA("¿Qué significa que se tire de la oreja?", "Suele significar que le duele algo. Enséñale los pictos del cuerpo y avisa a mamá.", "Mi idioma · lo añadiste tú"),
        QA("Se tapa los oídos y se ríe mucho", "Hay demasiado ruido. Llévale a un sitio tranquilo y ponle su música.", "Mi idioma · lo añadiste tú"),
        QA("Mañana vamos a la playa", "Come triturado: lleva su vaso con tapa. Le encanta el agua: vigílale de cerca.", "Comida y Me gusta · lo añadiste tú"),
    )

    val access = listOf(
        AccessPerson("javier", "J", "Javier · papá", "Puede editar"),
        AccessPerson("carmen", "C", "Carmen · abuela", "Siempre"),
        AccessPerson("cee", "S", "CEE San José", "14 personas"),
        AccessPerson("dani", "D", "Dani · monitor", "Caduca en 5 días", warn = true),
    )

    val durations = listOf("1 día", "1 semana", "1 mes", "Todo el curso")
    val roles = listOf("Ver" to "Solo ver", "Proponer" to "Ver y proponer")

    val proposals = listOf(
        Proposal(
            "laura", "L", "laura", "Laura", "Logopeda", "hoy",
            "Hoy se ha tocado el pecho para decir \"yo\".", "Mi idioma", "Me toco el pecho", "Quiero decir \"yo\"",
        ),
        Proposal(
            "dani", "D", null, "Dani", "Monitor del respiro", "ayer",
            "Últimamente se chupa el dedo; creo que es por estrés.", "Mi idioma", "Me chupo el dedo", "Puede que esté nervioso",
        ),
    )

    val qrOptions = listOf(
        QrOption("nombre", "Nombre y \"no hablo\"", true),
        QrOption("sino", "Cómo digo sí y no", true),
        QrOption("epilepsia", "Epilepsia", true),
        QrOption("familia", "Llamar a la familia", true),
        QrOption("diagnostico", "Diagnóstico", false),
    )

    val ages = listOf("0-5", "6-9", "10-14", "15+")
}
