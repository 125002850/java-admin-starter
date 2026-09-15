package com.oigit.admin.workcalendar.domain.model;

public record PublicationPreparation(
        Long draftVersionId,
        Integer draftLockVersion,
        String contentHash,
        PublicationDiff diff
) {
}
