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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Bean
public class Proxy implements MTurk {
    private static final Logger log = LogManager.getLogger(Proxy.class); //todo: log4j server config file

    private static final String SANDBOX_ENDPOINT = "mturk-requester-sandbox.us-east-1.amazonaws.com";
    private static final String PROD_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";
    private static final String SIGNING_REGION = "us-east-1";

    private static final String QUESTION_FILE = ""; //todo: create project template

    private AmazonMTurk client;

    public Proxy() {
        this.client = sandbox();
    }

    private static AmazonMTurk sandbox() {
        AmazonMTurkClientBuilder builder = AmazonMTurkClientBuilder.standard();
        builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(SANDBOX_ENDPOINT, SIGNING_REGION));
        return builder.build();
    }

    private static AmazonMTurk production() {
        AmazonMTurkClientBuilder builder = AmazonMTurkClientBuilder.standard();
        builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(PROD_ENDPOINT, SIGNING_REGION));
        return builder.build();
    }

    @Override
    public void createHits(Collection<String> msg) {
        msg.stream().map(id -> { try {return createHIT(QUESTION_FILE, id, client);} catch(IOException e) {log.error(e);} return null;});
    }

    @Override
    public List<SBtuple> processReviewablesHits() {
        return collectReviweableHits().stream().map(hit -> processHit(hit)).collect(Collectors.toList());
    }

    private Collection<HIT> collectReviweableHits() {
        ListReviewableHITsRequest r = new ListReviewableHITsRequest();
        return client.listReviewableHITs(r).getHITs();
    }

    private SBtuple processHit(HIT hit) {
        ListAssignmentsForHITRequest r = new ListAssignmentsForHITRequest();
        r.setHITId(hit.getHITId());
        List<Assignment> as = client.listAssignmentsForHIT(r).getAssignments();
        if (as.size() < 3) {
            log.error("less than 3 assignments");
        }
        boolean r1 = as.get(0).getAnswer().equalsIgnoreCase("yes"),
                r2 = as.get(1).getAnswer().equalsIgnoreCase("yes"),
                r3 = as.get(2).getAnswer().equalsIgnoreCase("yes");

        approveAssignments(as);
        return SBtuple.of(hit.getRequesterAnnotation(), r1 && r2 || r1 && r3 || r2 && r3);
    }

    private String createHIT(final String questionXmlFile, String slf, AmazonMTurk client) throws IOException {

        // QualificationRequirement: almost anyone can work on the task
        QualificationRequirement localeRequirement = new QualificationRequirement();
        localeRequirement.setQualificationTypeId("3PUFTE5I6SP7VKCILOPX6N3IGEHW2E");
        localeRequirement.setIntegerValues(List.of(0));
        localeRequirement.setComparator(Comparator.GreaterThan);
        localeRequirement.setRequiredToPreview(true);

        // Read the question XML into a String
        String questionSample = new String(Files.readAllBytes(Paths.get(questionXmlFile)));

        CreateHITRequest request = new CreateHITRequest();
        request.setMaxAssignments(3);
        request.setLifetimeInSeconds(600L);
        request.setAssignmentDurationInSeconds(600L);
        request.setRequesterAnnotation(slf);

        // Reward is a USD dollar amount - USD$0.20 in the example below
        request.setReward("0.20");
        request.setTitle("Compare faces");
        request.setKeywords("question, answer, moderation, verification");
        request.setDescription("Recognize faces, compare them, print 'yes' if they belong to the same person, 'no' otherwise");
        request.setQuestion(questionSample);

        CreateHITResult result = client.createHIT(request);
        return result.getHIT().getHITId();
    }

    private void approveAssignments(Collection<Assignment> a) {
        a.stream().forEach(x -> {
            log.debug("The worker with ID " + x.getWorkerId() + " submitted assignment "
                    + x.getAssignmentId() + " and gave the answer " + x.getAnswer());

            // Approve the assignment
            ApproveAssignmentRequest approveRequest = new ApproveAssignmentRequest();
            approveRequest.setAssignmentId(x.getAssignmentId());
            approveRequest.setRequesterFeedback("Good work, thank you!");
            approveRequest.setOverrideRejection(false);
            log.debug("Assignment has been approved: " + x.getAssignmentId());
        });
    }
}
