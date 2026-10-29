package com.verdant.app.data

import com.google.common.truth.Truth.assertThat
import com.verdant.app.data.knowledge.InMemoryKnowledgeBase
import com.verdant.app.data.knowledge.JsonSource
import org.junit.Test
import java.io.File

/** Runs against the real JSON shipped in the app, so it also guards data integrity. */
class KnowledgeBaseTest {

    private val kb = InMemoryKnowledgeBase {
        JsonSource(
            plantsJson = File("src/main/assets/knowledge/plants.json").readText(),
            diseasesJson = File("src/main/assets/knowledge/diseases.json").readText(),
        )
    }

    @Test
    fun `finds plants by id, alias, scientific and common name`() {
        assertThat(kb.findPlant("monstera_deliciosa")?.commonName).isEqualTo("Monstera")
        assertThat(kb.findPlant("Swiss cheese plant")?.id).isEqualTo("monstera_deliciosa")
        assertThat(kb.findPlant("Solanum lycopersicum")?.id).isEqualTo("solanum_lycopersicum")
        assertThat(kb.findPlant("Snake Plant")?.id).isEqualTo("dracaena_trifasciata")
        assertThat(kb.findPlant("Pepper,_bell")?.id).isEqualTo("capsicum_annuum")
    }

    @Test
    fun `falls back to genus and species for varieties`() {
        assertThat(kb.findPlant("Monstera deliciosa var albo")?.id).isEqualTo("monstera_deliciosa")
    }

    @Test
    fun `unknown plant returns null`() {
        assertThat(kb.findPlant("Welwitschia mirabilis")).isNull()
    }

    @Test
    fun `finds diseases from PlantVillage style labels`() {
        assertThat(kb.findDisease("Tomato___Early_blight")?.id).isEqualTo("early_blight")
        assertThat(kb.findDisease("Tomato___Tomato_Yellow_Leaf_Curl_Virus")?.id).isEqualTo("yellow_leaf_curl_virus")
        assertThat(kb.findDisease("Tomato___Spider_mites Two-spotted_spider_mite")?.id).isEqualTo("spider_mites")
        assertThat(kb.findDisease("powdery_mildew")?.id).isEqualTo("powdery_mildew")
    }

    @Test
    fun `healthy labels never map to a disease`() {
        assertThat(kb.findDisease("healthy")).isNull()
        assertThat(kb.findDisease("Tomato___healthy")).isNull()
    }

    @Test
    fun `data integrity - ids unique and hosts exist`() {
        val plantIds = kb.plants.map { it.id }
        assertThat(plantIds).containsNoDuplicates()
        assertThat(kb.diseases.map { it.id }).containsNoDuplicates()
        kb.diseases.forEach { disease ->
            disease.hosts.forEach { host -> assertThat(plantIds).contains(host) }
            assertThat(disease.treatment).isNotEmpty()
            assertThat(disease.symptoms).isNotEmpty()
        }
        kb.plants.forEach { plant ->
            assertThat(plant.care).isNotNull()
            assertThat(kb.diseases.any { plant.id in it.hosts }).isTrue()
        }
    }
}
