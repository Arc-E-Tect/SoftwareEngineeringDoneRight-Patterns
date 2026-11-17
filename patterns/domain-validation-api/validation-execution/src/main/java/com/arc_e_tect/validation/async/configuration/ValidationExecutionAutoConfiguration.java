package com.arc_e_tect.validation.async.configuration;

import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecution;
import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecutionProblems;
import com.arc_e_tect.validation.async.adapter.outbound.memory.InMemoryValidationJobStore;
import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.service.GetValidationJobService;
import com.arc_e_tect.validation.async.application.domain.service.SubmitValidationJobService;
import com.arc_e_tect.validation.async.application.port.inbound.GetValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.SubmitValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationJobStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;

/**
 * Sets up the asynchronous execution in a servlet web application: the use cases, the
 * {@link ValidationExecution} a validation's controller uses, and an in-memory job store unless the
 * bounded context supplies its own {@link ValidationJobStore}. Jobs run on virtual threads.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ValidationExecutionProperties.class)
public class ValidationExecutionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ValidationJobStore validationJobStore() {
        return new InMemoryValidationJobStore();
    }

    @Bean
    public SubmitValidationJobUseCase submitValidationJobUseCase(ValidationJobStore store, ValidationExecutionProperties properties) {
        return new SubmitValidationJobService(store, Executors.newVirtualThreadPerTaskExecutor(), unexpectedFailure(properties));
    }

    @Bean
    public GetValidationJobUseCase getValidationJobUseCase(ValidationJobStore store) {
        return new GetValidationJobService(store);
    }

    @Bean
    public ValidationExecution validationExecution(SubmitValidationJobUseCase submit, GetValidationJobUseCase get,
                                                   ValidationExecutionProperties properties) {
        return new ValidationExecution(submit, get, new ValidationExecutionProblems(properties.problemTypes()), properties.retryAfter());
    }

    /** The problem of a job whose validation failed in a way it did not answer with a problem of its own. */
    JobProblem unexpectedFailure(ValidationExecutionProperties properties) {
        return new JobProblem(properties.problemTypes().resolve("internal-server-error"), "Internal Server Error", 500,
                "An unexpected error occurred while running the validation.");
    }
}
