package com.oigit.admin.iam.domain.gateway;

import com.oigit.admin.iam.domain.model.ClientRequestInfo;

public interface ClientRequestGateway {
    ClientRequestInfo current();
}
