package br.com.gym.flow.api.error;

import java.util.Map;

/**
 * Builds friendly Portuguese validation messages by combining a humanized
 * field label with the violated constraint. Falls back to the default
 * Bean Validation message when the constraint is unknown.
 */
final class ValidationMessages {

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
        Map.entry("name", "nome"),
        Map.entry("email", "e-mail"),
        Map.entry("password", "senha"),
        Map.entry("phoneNumber", "telefone"),
        Map.entry("postalCode", "CEP"),
        Map.entry("street", "rua"),
        Map.entry("number", "número"),
        Map.entry("complement", "complemento"),
        Map.entry("district", "bairro"),
        Map.entry("city", "cidade"),
        Map.entry("neighborhoodId", "bairro de entrega"),
        Map.entry("productId", "produto"),
        Map.entry("variationId", "variação"),
        Map.entry("categoryId", "categoria"),
        Map.entry("orderId", "pedido"),
        Map.entry("tableId", "mesa"),
        Map.entry("comandaId", "comanda"),
        Map.entry("idToken", "ID token"),
        Map.entry("refreshToken", "refresh token"),
        Map.entry("cardToken", "token do cartão"),
        Map.entry("code", "código"),
        Map.entry("rating", "nota"),
        Map.entry("comment", "comentário"),
        Map.entry("quantity", "quantidade"),
        Map.entry("observation", "observação"),
        Map.entry("displayOrder", "ordem de exibição"),
        Map.entry("basePrice", "preço base"),
        Map.entry("priceModifier", "diferença de preço"),
        Map.entry("price", "preço"),
        Map.entry("imageUrl", "URL da imagem"),
        Map.entry("description", "descrição"),
        Map.entry("openTime", "horário de abertura"),
        Map.entry("closeTime", "horário de fechamento"),
        Map.entry("hoursByDay", "horário por dia"),
        Map.entry("fee", "taxa"),
        Map.entry("currency", "moeda"),
        Map.entry("validFrom", "início da validade"),
        Map.entry("validUntil", "fim da validade"),
        Map.entry("minOrderValue", "valor mínimo do pedido"),
        Map.entry("maxUses", "limite de usos"),
        Map.entry("maxSelection", "seleção máxima"),
        Map.entry("minSelection", "seleção mínima"),
        Map.entry("type", "tipo"),
        Map.entry("value", "valor"),
        Map.entry("status", "status"),
        Map.entry("modality", "modalidade"),
        Map.entry("method", "método"),
        Map.entry("token", "token")
    );

    private ValidationMessages() {}

    static String humanize(String field, String code, Object[] args, String defaultMessage) {
        String label = label(field);

        return switch (code == null ? "" : code) {
            case "NOTBLANK", "NOTEMPTY" -> "O campo '" + label + "' não pode ficar vazio.";
            case "NOTNULL" -> "O campo '" + label + "' é obrigatório.";
            case "EMAIL" -> "O '" + label + "' está em formato inválido.";
            case "SIZE" -> sizeMessage(label, args);
            case "MIN" -> "O campo '" + label + "' deve ser maior ou igual a " + arg(args, 1) + ".";
            case "MAX" -> "O campo '" + label + "' deve ser menor ou igual a " + arg(args, 1) + ".";
            case "POSITIVE" -> "O campo '" + label + "' deve ser positivo.";
            case "POSITIVEORZERO" -> "O campo '" + label + "' não pode ser negativo.";
            case "NEGATIVE" -> "O campo '" + label + "' deve ser negativo.";
            case "NEGATIVEORZERO" -> "O campo '" + label + "' não pode ser positivo.";
            case "DECIMALMIN" -> "O campo '" + label + "' deve ser maior ou igual a " + arg(args, 1) + ".";
            case "DECIMALMAX" -> "O campo '" + label + "' deve ser menor ou igual a " + arg(args, 1) + ".";
            case "DIGITS" -> "O campo '" + label + "' deve ter no máximo " + arg(args, 1) + " dígitos inteiros e " + arg(args, 2) + " decimais.";
            case "PATTERN" -> "O campo '" + label + "' está em formato inválido.";
            case "PAST" -> "O campo '" + label + "' deve ser uma data passada.";
            case "FUTURE" -> "O campo '" + label + "' deve ser uma data futura.";
            case "TYPEMISMATCH" -> "Valor inválido para o campo '" + label + "'.";
            default -> defaultMessage == null
                ? "O campo '" + label + "' é inválido."
                : "O campo '" + label + "' " + defaultMessage + ".";
        };
    }

    private static String sizeMessage(String label, Object[] args) {
        // Bean Validation Size args: [labelMessageSourceResolvable, max, min]
        Object max = arg(args, 1);
        Object min = arg(args, 2);
        if (min == null && max == null) {
            return "O campo '" + label + "' tem tamanho inválido.";
        }
        if ("0".equals(String.valueOf(min)) && max != null) {
            return "O campo '" + label + "' deve ter no máximo " + max + " caracteres.";
        }
        if (min != null && (max == null || Integer.MAX_VALUE == toIntOrMin(max))) {
            return "O campo '" + label + "' deve ter no mínimo " + min + " caracteres.";
        }
        return "O campo '" + label + "' deve ter entre " + min + " e " + max + " caracteres.";
    }

    private static int toIntOrMin(Object o) {
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private static Object arg(Object[] args, int index) {
        if (args == null || args.length <= index) return null;
        return args[index];
    }

    private static String label(String field) {
        if (field == null || field.isBlank()) return "campo";
        return FIELD_LABELS.getOrDefault(field, field);
    }
}
