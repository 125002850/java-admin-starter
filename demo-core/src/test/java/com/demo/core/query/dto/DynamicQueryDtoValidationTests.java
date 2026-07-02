package com.demo.core.query.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicQueryDtoValidationTests {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void paged_dynamic_query_should_inherit_default_page_values() {
        DemoPagedReqDTO reqDTO = new DemoPagedReqDTO();

        assertThat(reqDTO.getPageNo()).isEqualTo(1);
        assertThat(reqDTO.getPageSize()).isEqualTo(20);
    }

    @Test
    void text_condition_should_require_value_for_non_in_operator() {
        TextConditionDTO<TextField> condition = new TextConditionDTO<>();
        condition.setField(TextField.DICT_TYPE_NAME);
        condition.setOp(TextConditionDTO.TextOperator.CONTAINS);

        Set<ConstraintViolation<TextConditionDTO<TextField>>> violations = VALIDATOR.validate(condition);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void text_condition_should_require_values_for_in_operator() {
        TextConditionDTO<TextField> condition = new TextConditionDTO<>();
        condition.setField(TextField.DICT_TYPE_CODE);
        condition.setOp(TextConditionDTO.TextOperator.IN);
        condition.setValues(List.of());

        Set<ConstraintViolation<TextConditionDTO<TextField>>> violations = VALIDATOR.validate(condition);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void text_condition_should_accept_null_operator_without_value() {
        TextConditionDTO<TextField> condition = new TextConditionDTO<>();
        condition.setField(TextField.DICT_TYPE_CODE);
        condition.setOp(TextConditionDTO.TextOperator.IS_NULL);

        Set<ConstraintViolation<TextConditionDTO<TextField>>> violations = VALIDATOR.validate(condition);

        assertThat(violations).isEmpty();
    }

    @Test
    void datetime_condition_should_require_start_and_end_for_between() {
        DateTimeConditionDTO<DateTimeField> condition = new DateTimeConditionDTO<>();
        condition.setField(DateTimeField.CREATE_TIME);
        condition.setOp(DateTimeConditionDTO.DateTimeOperator.BETWEEN);
        condition.setStart("2026-06-01 00:00:00");

        Set<ConstraintViolation<DateTimeConditionDTO<DateTimeField>>> violations = VALIDATOR.validate(condition);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void datetime_condition_should_accept_null_operator_without_value() {
        DateTimeConditionDTO<DateTimeField> condition = new DateTimeConditionDTO<>();
        condition.setField(DateTimeField.CREATE_TIME);
        condition.setOp(DateTimeConditionDTO.DateTimeOperator.IS_NOT_NULL);

        Set<ConstraintViolation<DateTimeConditionDTO<DateTimeField>>> violations = VALIDATOR.validate(condition);

        assertThat(violations).isEmpty();
    }

    @Test
    void enum_condition_should_require_value_for_eq_operator() {
        EnumConditionDTO<EnumField, Integer> condition = new EnumConditionDTO<>();
        condition.setField(EnumField.STATUS);
        condition.setOp(EnumConditionDTO.EnumOperator.EQ);

        Set<ConstraintViolation<EnumConditionDTO<EnumField, Integer>>> violations = VALIDATOR.validate(condition);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void enum_condition_should_accept_null_operator_without_value() {
        EnumConditionDTO<EnumField, Integer> condition = new EnumConditionDTO<>();
        condition.setField(EnumField.STATUS);
        condition.setOp(EnumConditionDTO.EnumOperator.IS_NULL);

        Set<ConstraintViolation<EnumConditionDTO<EnumField, Integer>>> violations = VALIDATOR.validate(condition);

        assertThat(violations).isEmpty();
    }

    @Test
    void paged_dynamic_query_should_limit_sort_items() {
        DemoPagedReqDTO reqDTO = new DemoPagedReqDTO();
        reqDTO.setSort(List.of(
            sort(SortField.ID),
            sort(SortField.CREATE_TIME),
            sort(SortField.UPDATE_TIME),
            sort(SortField.STATUS)
        ));

        Set<ConstraintViolation<DemoPagedReqDTO>> violations = VALIDATOR.validate(reqDTO);

        assertThat(violations).isNotEmpty();
    }

    private DemoSortItem sort(SortField field) {
        DemoSortItem sortItem = new DemoSortItem();
        sortItem.setField(field);
        sortItem.setDirection(SortItemDTO.SortDirection.ASC);
        return sortItem;
    }

    private enum TextField {
        DICT_TYPE_CODE,
        DICT_TYPE_NAME
    }

    private enum DateTimeField {
        CREATE_TIME
    }

    private enum EnumField {
        STATUS
    }

    private enum SortField {
        ID,
        CREATE_TIME,
        UPDATE_TIME,
        STATUS
    }

    private static final class DemoConditionNode implements AbstractConditionNodeDTO {
    }

    private static final class DemoSortItem extends SortItemDTO<SortField> {
    }

    private static final class DemoCriteriaReqDTO extends BaseDynamicCriteriaReqDTO<DemoConditionNode, DemoSortItem> {
    }

    private static final class DemoPagedReqDTO extends BasePagedDynamicQueryReqDTO<DemoConditionNode, DemoSortItem> {
    }
}
