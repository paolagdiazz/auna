package com.auna.padres.data

/** Estructura fija del pasaporte (secciones base y sus apartados). */
object Templates {
    fun baseSections() = listOf(
        Section("sobre", "Sobre mí", listOf(Group("yo", "Yo"), Group("gente", "Mi gente"))),
        Section("idioma", "Mi idioma"),
        Section("calma", "Qué me calma", listOf(Group("calma", "Me calma"), Group("altera", "Me altera", warm = true))),
        Section("gusta", "Me gusta", listOf(Group("gusta", "Me encanta"))),
        Section("comida", "Comida", listOf(Group("cuidado", "Cuidado", warm = true), Group("como", "Cómo como"))),
        Section("salud", "Salud y cuidados", listOf(Group("salud", "Salud"), Group("crisis", "Si tengo una crisis", warm = true)), onlyFamily = true),
    )

    val baseIds = listOf("sobre", "idioma", "calma", "gusta", "comida", "salud")

    fun customSection(title: String) = Section(newId("s"), title, listOf(Group(newId("g"), title)), custom = true)

    val qrOptions = listOf(
        QrOption("nombre", "Nombre, edad y \"no hablo\"", true),
        QrOption("sino", "Cómo digo sí y no", true),
        QrOption("crisis", "Qué hacer en una crisis", true),
        QrOption("familia", "Teléfonos de la familia", true),
        QrOption("salud", "Salud y diagnóstico", false),
    )

    fun qrDefaults() = qrOptions.associate { it.key to it.defaultOn }

    val extraSectionOptions = listOf("Colegio", "Sueño", "Higiene", "Rutinas")
}

/** Datos de ejemplo: Marco (pasaporte completo) y Lucas (casi vacío). */
object Sample {
    const val MARCO = "marco"
    private const val DAY = 24L * 60 * 60 * 1000

    fun kids(): List<Kid> {
        val base = Templates.baseSections()
        fun fill(id: String, vararg groups: Pair<String, List<String>>) =
            base.first { it.id == id }.let { s ->
                s.copy(groups = s.groups.map { g -> groups.firstOrNull { it.first == g.id }?.let { g.copy(items = it.second) } ?: g })
            }
        val marcoSections = listOf(
            fill("sobre", "yo" to listOf("Me llamo Marco", "9 años", "No hablo"), "gente" to listOf("Mamá · Ana", "Papá · Javier", "Mi hermana Lucía")),
            base.first { it.id == "idioma" },
            fill("calma", "calma" to listOf("El agua", "Música de pelis", "Hablarme bajito", "Mi mantita azul"), "altera" to listOf("Ruidos fuertes", "Mucha gente", "Cogerme de golpe", "Esperar mucho")),
            fill("gusta", "gusta" to listOf("El agua", "La música", "Los columpios", "Mi hermana Lucía", "Pelotas con ruido")),
            fill("comida", "cuidado" to listOf("Todo triturado", "Me atraganto con trozos"), "como" to listOf("Vaso con tapa", "Necesito ayuda", "Sin alergias")),
            fill("salud", "salud" to listOf("Síndrome de Angelman", "Epilepsia"), "crisis" to listOf("Llama al 112", "Ponme de lado", "No me sujetes", "Nada en la boca", "Mira cuánto dura")),
        )
        val marco = Kid(
            MARCO, "Marco", "9 años", photo = "marco",
            passport = Passport(
                noHabla = true,
                highlights = listOf("Como triturado", "Los ruidos fuertes me alteran", "Si me río, no siempre estoy contento"),
                yes = "Sonrío y toco tu mano",
                no = "Giro la cabeza",
                signals = listOf(
                    Signal("s1", "Me río mucho", "No siempre estoy contento", "Fíjate en lo demás que hago."),
                    Signal("s2", "Me tiro de la oreja y lloro", "Me duele algo", "Pictos del cuerpo. Avisa a mamá."),
                    Signal("s3", "Me tapo los oídos y me río fuerte", "Hay mucho ruido", "Sitio tranquilo y mi música."),
                    Signal("s4", "Te llevo a la nevera", "Tengo hambre o sed", "Pictos de comida y bebida."),
                    Signal("s5", "Doy palmas mirando la puerta", "Quiero salir", "Si se puede, sal conmigo."),
                    Signal("s6", "Me muerdo la mano", "Estoy nervioso", "Voz bajita y mi juguete de agua."),
                    Signal("s7", "Me froto los ojos y me tumbo", "Tengo sueño", "Déjame descansar tranquilo."),
                ),
                sections = marcoSections,
                contacts = listOf(Contact("c1", "Ana", "Mamá"), Contact("c2", "Javier", "Papá")),
            ),
            access = listOf(
                Access("javier", "Javier", "Papá", PERM_EDIT),
                Access("carmen", "Carmen", "Abuela", PERM_VIEW),
                Access("cee", "CEE San José", "Colegio", PERM_PROPOSE, members = 14),
                Access("dani", "Dani", "Monitor", PERM_PROPOSE, until = System.currentTimeMillis() + 5 * DAY),
            ),
        )
        val lucas = Kid(
            "lucas", "Lucas", "7 años", photo = "lucas",
            passport = Passport(
                sections = base.map { s ->
                    when (s.id) {
                        "sobre" -> s.copy(groups = s.groups.map { if (it.id == "yo") it.copy(items = listOf("Me llamo Lucas", "7 años")) else it })
                        "gusta" -> s.copy(groups = s.groups.map { it.copy(items = listOf("Los dinosaurios", "Dibujar")) })
                        else -> s
                    }
                },
                yes = "Digo «sí» con la cabeza",
                no = "Me voy",
            ),
        )
        return listOf(marco, lucas)
    }

    fun requests() = listOf(AccessRequest("pedro", MARCO, "Pedro Ruiz", "Profe sustituto", "CEE San José"))

    fun proposals() = listOf(
        Proposal("laura", MARCO, "Laura", "Logopeda", "hoy", "Hoy se ha tocado el pecho para decir \"yo\".", "Me toco el pecho", "Quiero decir \"yo\"", photo = "laura"),
        Proposal("dani", MARCO, "Dani", "Monitor del respiro", "ayer", "Últimamente se chupa el dedo; creo que es por estrés.", "Me chupo el dedo", "Puede que esté nervioso"),
    )

    // --- Usado por pantallas que aún no están conectadas a Claude ---
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
    val durations = listOf("1 día", "1 semana", "1 mes", "Todo el curso")
    val ages = listOf("0-5", "6-9", "10-14", "15+")
}
