package com.sumsubstance;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.mturk.AmazonMTurk;
import com.amazonaws.services.mturk.AmazonMTurkClientBuilder;
import com.amazonaws.services.mturk.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Proxy {
    private static final Logger log = LogManager.getLogger(Proxy.class); //todo: log4j server config file

    private static final String SANDBOX_ENDPOINT = "mturk-requester-sandbox.us-east-1.amazonaws.com";
    private static final String PROD_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";
    private static final String SIGNING_REGION = "us-east-1";

    private static final String QUESTION_FILE = "my_question.xml";
    private static final long LIFETIME_IN_SECONDS = 600L;

    private AmazonMTurk client;

    private Proxy(AmazonMTurk client) {
        this.client = client;
    }

    public static Proxy sandbox() {
        AmazonMTurkClientBuilder builder = AmazonMTurkClientBuilder.standard();
        builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(SANDBOX_ENDPOINT, SIGNING_REGION));
        return new Proxy(builder.build());
    }

    public static Proxy production() {
        AmazonMTurkClientBuilder builder = AmazonMTurkClientBuilder.standard();
        builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(PROD_ENDPOINT, SIGNING_REGION));
        return new Proxy(builder.build());
    }

    public void submit(Tuple<String, String> h) throws Exception {
        createHIT(QUESTION_FILE, h.getLeft(), client, h.getRight());
    }

    public void approve(Processing.Assignment x) {
        log.debug("The worker with ID " + x.getWorkerId() + " submitted assignment "
                + x.getAsnId() + " and gave the answer " + x.isResult());

        // Approve the assignment
        ApproveAssignmentRequest approveRequest = new ApproveAssignmentRequest();
        approveRequest.setAssignmentId(x.getAsnId());
        approveRequest.setRequesterFeedback("Good work, thank you!");
        approveRequest.setOverrideRejection(false);
        client.approveAssignment(approveRequest);
        log.debug("Assignment has been approved: " + x.getAsnId());
    }

    public Collection<Processing.Assignment> available() {
        return client.listReviewableHITs(new ListReviewableHITsRequest()).getHITs().stream().
                map((h)-> {
                    final ListAssignmentsForHITRequest r = new ListAssignmentsForHITRequest();
                    r.setHITId(h.getHITId());
                    r.setAssignmentStatuses(Collections.singletonList(AssignmentStatus.Submitted.name()));
                    r.setMaxResults(20);
                    return client.listAssignmentsForHIT(r).getAssignments().stream().map((a) -> toAssignment(h, a));
                }).flatMap(Function.identity()).collect(Collectors.toList());
    }

    private static Processing.Assignment toAssignment(HIT hit, com.amazonaws.services.mturk.model.Assignment a) {
        return new Processing.Assignment(hit.getHITId(), a.getAssignmentId(), a.getWorkerId(), hit.getExpiration().getTime(), hit.getRequesterAnnotation(), "yes".equalsIgnoreCase(a.getAnswer()));
    }

    private HIT createHIT(final String questionXmlFile, String slf, AmazonMTurk client, String imageUrl) throws IOException {

        // QualificationRequirement: almost anyone can work on the task
        QualificationRequirement localeRequirement = new QualificationRequirement();
        localeRequirement.setQualificationTypeId("3PUFTE5I6SP7VKCILOPX6N3IGEHW2E");
        localeRequirement.setIntegerValues(Arrays.asList(0));
        localeRequirement.setComparator(Comparator.GreaterThan);
        localeRequirement.setRequiredToPreview(true);

        // Read the question XML into a String
        String questionSample = new String(Files.readAllBytes(Paths.get(questionXmlFile)));

        CreateHITRequest request = new CreateHITRequest();
        request.setMaxAssignments(Processing.MAX_ASSIGNMENTS);
        request.setLifetimeInSeconds(LIFETIME_IN_SECONDS);
        request.setAssignmentDurationInSeconds(600L);
        request.setRequesterAnnotation(slf);

        // Reward is a USD dollar amount - USD$0.20 in the example below
        request.setReward("0.20");
        request.setTitle("Compare faces");
        request.setKeywords("question, answer, moderation, verification");
        request.setDescription("Recognize faces, compare them, print 'yes' if they belong to the same person, 'no' otherwise");
        request.setQuestion(questionSample);

        CreateHITResult result = client.createHIT(request);
        System.out.println(String.format("%s hit created)", result.getHIT().getHITId()));
        return result.getHIT();
    }
}
