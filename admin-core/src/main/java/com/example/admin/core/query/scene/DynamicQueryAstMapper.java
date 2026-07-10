package com.example.admin.core.query.scene;

import com.example.admin.core.exception.BizException;
import com.example.admin.core.query.ast.ConditionAstNode;
import com.example.admin.core.query.ast.ConditionGroupAst;
import com.example.admin.core.query.ast.ConditionLeafAst;
import com.example.admin.core.query.ast.QueryAst;
import com.example.admin.core.query.ast.QueryLogicOperator;
import com.example.admin.core.query.ast.QueryOperator;
import com.example.admin.core.query.ast.SortSpec;
import com.example.admin.core.query.dto.AbstractConditionNodeDTO;
import com.example.admin.core.query.dto.BasePagedDynamicQueryReqDTO;
import com.example.admin.core.query.dto.ConditionGroupDTO;
import com.example.admin.core.query.dto.DateTimeConditionDTO;
import com.example.admin.core.query.dto.EnumConditionDTO;
import com.example.admin.core.query.dto.SortItemDTO;
import com.example.admin.core.query.dto.TextConditionDTO;
import com.example.admin.core.query.exception.DynamicQueryErrorCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DynamicQueryAstMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DynamicQueryAstMapper() {
    }

    public static QueryAst toQueryAst(
            AbstractConditionNodeDTO condition,
            List<? extends SortItemDTO<?>> sortItems
    ) {
        QueryAst queryAst = new QueryAst();
        queryAst.setRoot(toNode(condition));
        queryAst.setSorts(toSorts(sortItems));
        return queryAst;
    }

    public static QueryAst toPageQueryAst(BasePagedDynamicQueryReqDTO<?, ?> pageReqDTO) {
        QueryAst queryAst = toQueryAst(pageReqDTO.getCondition(), pageReqDTO.getSort());
        queryAst.setPageNo(pageReqDTO.getPageNo());
        queryAst.setPageSize(pageReqDTO.getPageSize());
        return queryAst;
    }

    public static ConditionAstNode toNode(AbstractConditionNodeDTO node) {
        if (node == null) {
            return null;
        }
        if (node instanceof ConditionGroupDTO<?> groupCondition) {
            return toGroup(groupCondition);
        }
        if (node instanceof TextConditionDTO<?> textCondition) {
            return toTextLeaf(textCondition);
        }
        if (node instanceof DateTimeConditionDTO<?> dateTimeCondition) {
            return toDateTimeLeaf(dateTimeCondition);
        }
        if (node instanceof EnumConditionDTO<?, ?> enumCondition) {
            return toEnumLeaf(enumCondition);
        }
        throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_UNSUPPORTED_NODE);
    }

    public static List<SortSpec> toSorts(List<? extends SortItemDTO<?>> sortItems) {
        if (sortItems == null || sortItems.isEmpty()) {
            return List.of();
        }
        return sortItems.stream()
                .map(sortItem -> new SortSpec(sortItem.getField().name(), toDirection(sortItem.getDirection())))
                .toList();
    }

    private static ConditionGroupAst toGroup(ConditionGroupDTO<?> groupCondition) {
        ConditionGroupAst groupAst = new ConditionGroupAst();
        groupAst.setLogic(groupCondition.getLogic() == ConditionGroupDTO.LogicOperator.OR
                ? QueryLogicOperator.OR
                : QueryLogicOperator.AND);
        List<?> children = groupCondition.getChildren();
        if (children != null) {
            groupAst.setChildren(children.stream()
                    .map(AbstractConditionNodeDTO.class::cast)
                    .map(DynamicQueryAstMapper::toNode)
                    .toList());
        }
        return groupAst;
    }

    private static ConditionLeafAst toTextLeaf(TextConditionDTO<?> textCondition) {
        ConditionLeafAst leafAst = new ConditionLeafAst();
        leafAst.setFieldKey(textCondition.getField().name());
        leafAst.setOperator(toTextOperator(textCondition.getOp()));
        leafAst.setTypedValue(switch (textCondition.getOp()) {
            case IN -> textCondition.getValues();
            case IS_NULL, IS_NOT_NULL -> null;
            default -> textCondition.getValue();
        });
        return leafAst;
    }

    private static ConditionLeafAst toDateTimeLeaf(DateTimeConditionDTO<?> dateTimeCondition) {
        ConditionLeafAst leafAst = new ConditionLeafAst();
        leafAst.setFieldKey(dateTimeCondition.getField().name());
        leafAst.setOperator(toDateTimeOperator(dateTimeCondition.getOp()));
        leafAst.setTypedValue(switch (dateTimeCondition.getOp()) {
            case BETWEEN -> List.of(parse(dateTimeCondition.getStart()), parse(dateTimeCondition.getEnd()));
            case IS_NULL, IS_NOT_NULL -> null;
            default -> parse(dateTimeCondition.getValue());
        });
        return leafAst;
    }

    private static ConditionLeafAst toEnumLeaf(EnumConditionDTO<?, ?> enumCondition) {
        ConditionLeafAst leafAst = new ConditionLeafAst();
        leafAst.setFieldKey(enumCondition.getField().name());
        leafAst.setOperator(toEnumOperator(enumCondition.getOp()));
        leafAst.setTypedValue(switch (enumCondition.getOp()) {
            case IN -> enumCondition.getValues();
            case IS_NULL, IS_NOT_NULL -> null;
            default -> enumCondition.getValue();
        });
        return leafAst;
    }

    private static QueryOperator toTextOperator(TextConditionDTO.TextOperator operator) {
        return switch (operator) {
            case EQ -> QueryOperator.EQ;
            case CONTAINS -> QueryOperator.CONTAINS;
            case STARTS_WITH -> QueryOperator.STARTS_WITH;
            case ENDS_WITH -> QueryOperator.ENDS_WITH;
            case IN -> QueryOperator.IN;
            case IS_NULL -> QueryOperator.IS_NULL;
            case IS_NOT_NULL -> QueryOperator.IS_NOT_NULL;
        };
    }

    private static QueryOperator toDateTimeOperator(DateTimeConditionDTO.DateTimeOperator operator) {
        return switch (operator) {
            case GT -> QueryOperator.GT;
            case GTE -> QueryOperator.GTE;
            case LT -> QueryOperator.LT;
            case LTE -> QueryOperator.LTE;
            case BETWEEN -> QueryOperator.BETWEEN;
            case IS_NULL -> QueryOperator.IS_NULL;
            case IS_NOT_NULL -> QueryOperator.IS_NOT_NULL;
        };
    }

    private static QueryOperator toEnumOperator(EnumConditionDTO.EnumOperator operator) {
        return switch (operator) {
            case EQ -> QueryOperator.EQ;
            case IN -> QueryOperator.IN;
            case IS_NULL -> QueryOperator.IS_NULL;
            case IS_NOT_NULL -> QueryOperator.IS_NOT_NULL;
        };
    }

    private static SortItemDTO.SortDirection toDirection(SortItemDTO.SortDirection direction) {
        return direction == SortItemDTO.SortDirection.DESC
                ? SortItemDTO.SortDirection.DESC
                : SortItemDTO.SortDirection.ASC;
    }

    private static LocalDateTime parse(String source) {
        return LocalDateTime.parse(source, DATE_TIME_FORMATTER);
    }
}
