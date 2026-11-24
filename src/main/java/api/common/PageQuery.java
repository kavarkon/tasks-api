package api.common;

import org.springframework.data.domain.Limit;

public record PageQuery(int limit, int afterId) {

    public static PageQuery parse(String limit, String afterId) {
        int defaultLimit = 50;
        int maximumLimit = 100;
        int pageSize = limit == null ? defaultLimit : parsePositiveInteger("limit", limit);
        if (pageSize > maximumLimit) {
            throw new InvalidQueryParameterException("limit");
        }
        int lowerBound = afterId == null ? 0 : parsePositiveInteger("afterId", afterId);
        return new PageQuery(pageSize, lowerBound);
    }

    public static int parsePositiveInteger(String name, String value) {
        try {
            int number = Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException exception) {
            throw new InvalidQueryParameterException(name);
        }
        throw new InvalidQueryParameterException(name);
    }

    public Limit fetchLimit() {
        return Limit.of(limit + 1);
    }
}
