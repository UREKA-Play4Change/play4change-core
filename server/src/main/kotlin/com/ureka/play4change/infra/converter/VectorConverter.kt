package com.ureka.play4change.infra.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class VectorConverter : AttributeConverter<FloatArray?, String?> {

    override fun convertToDatabaseColumn(attribute: FloatArray?): String? {
        if (attribute == null) return null
        // Returns the string format: [0.1,0.2,0.3]
        return attribute.joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    override fun convertToEntityAttribute(dbData: String?): FloatArray? {
        if (dbData == null) return null
        val cleanData = dbData.removeSurrounding("[", "]")
        if (cleanData.isEmpty()) return floatArrayOf()
        return cleanData.split(",").map { it.trim().toFloat() }.toFloatArray()
    }
}