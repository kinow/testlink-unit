/* 
 * The MIT License
 * 
 * Copyright (c) 2013 Matteo Castellarin, Bruno P. Kinoshita
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package testlink.testcase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI;
import br.eti.kinoshita.testlinkjavaapi.constants.ActionOnDuplicate;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionType;
import br.eti.kinoshita.testlinkjavaapi.constants.ResponseDetails;
import br.eti.kinoshita.testlinkjavaapi.constants.TestImportance;
import br.eti.kinoshita.testlinkjavaapi.model.Attachment;
import br.eti.kinoshita.testlinkjavaapi.model.CustomField;
import br.eti.kinoshita.testlinkjavaapi.model.Requirement;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestCaseStep;
import br.eti.kinoshita.testlinkjavaapi.model.TestProject;
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite;
import br.eti.kinoshita.testlinkjavaapi.util.TestLinkAPIException;

/**
 * Wrapper class that represents a TestLink instance.
 * 
 * <p>
 * This class is not supposed to be used by clients of this API.
 * </p>
 * 
 * <p>
 * This class is thread safe and not serializable
 * </p>
 * 
 * @author mcaste00
 * @since 0.1
 */
final class TestLinkSite {
    
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TestLinkSite.class.getName());
    
    /**
     * TestLink API. Used to communicate with TestLink.
     */
    private TestLinkAPI api = null;
    
    /**
     * Create an instance of the TestLink Java API.
     * 
     * <p>
     * URL format is: <code>http://&lt;server&gt;:&lt;port&gt;/testlink/lib/api/xmlrpc.php</code>
     * </p>
     * 
     * @param url address to the TestLink server
     * @param devKey developer key to the TestLink server
     * @throws RuntimeException if there is a problem with TestLink URL or creating its API obejct
     */
    /* package */ TestLinkSite(String url, String devKey) {
        // if there is no connection active at the moment
        if (api == null) {
            try {
                // get TestLink URL
                final URL testlinkURL = new URL(url);
                // create the TestLink API
                api = new TestLinkAPI(testlinkURL, devKey);
            } catch (MalformedURLException mue) {
                LOGGER.log(Level.SEVERE, "Impossible to establish a connection to the TestLink server. "
                        + "Check the parameters of the [" + this.getClass().getName() + "] class", mue);
                throw new RuntimeException("Connection problems with TestLink: " + mue.getMessage(), mue);
            } catch (TestLinkAPIException te) {
                LOGGER.log(Level.SEVERE, "Impossible to instantiate TestLink API", te);
                throw new RuntimeException("Internal error when creating TestLink API: " + te.getMessage(), te);
            }
        }
    }

    /**
     * Ping the connection of the TestLink API instance
     */
    /* package */ void pingTestLink() {
        LOGGER.log(Level.FINEST, "Answer to ping is: " + api.ping());
    }

    /**
     * Given a test project name returns its associated object.
     * 
     * @param testProjectName a test project name
     * @return the test project associated object
     */
    /* package */ TestProject getTestProject(String testProjectName) {
        TestProject[] testProjects = api.getProjects();

        for (TestProject testProject : testProjects) {
            String name = testProject.getName();
            if (name.equals(testProjectName)) {
                return testProject;
            }
        }

        return null;
    }

    /**
     * Given a test and list of requirements, binds them together
     * 
     * @param testCaseId the test case where to attach the requirements
     * @param requirements the requirements to be linked to the test case
     */
    /* package */ void assignRequirements(TestCase testCase, List<Requirement> requirements) {
        api.assignRequirements(testCase.getId(), testCase.getTestProjectId(), requirements);
    }

    /**
     * Returns a custom field given its Test Project and Test Case.
     * 
     * @param testProjectId the test project ID
     * @param testCaseId the test case ID
     * @param customFieldName the name of the custom field
     * @param versionNumber the custom field version number
     * @return the custom field
     */
    /* package */ CustomField getCustomField(Integer testProjectId, Integer testCaseId, String customFieldName,
            Integer versionNumber) {
        CustomField customField = api.getTestCaseCustomFieldDesignValue(testCaseId, null, versionNumber, testProjectId,
                customFieldName, ResponseDetails.FULL);

        return customField;
    }

    /**
     * Given a test suite name returns its associated object.
     * 
     * @param testProject the test project
     * @param testSuiteName a test suite name in the first level of the project
     * @return the test suite associated object or <code>null</code> if no test suite is found
     */
    /* package */ TestSuite getTestSuite(Integer testProject, String testSuiteName) {
        TestSuite[] testSuites = api.getFirstLevelTestSuitesForTestProject(testProject);

        for (TestSuite testSuite : testSuites) {
            String name = testSuite.getName();
            if (name.equals(testSuiteName)) {
                return testSuite;
            }
        }

        return null;
    }

    /**
     * Converts a list of test steps composed of couples action-expected result in a {@link TestCaseStep} instance for
     * the TestLink API.
     * 
     * @param steps the steps to be converted
     * @return the TestLink API representation for the test steps
     * @throws TestLinkJavaAPI for invalid input fields
     */
    /* package */ List<TestCaseStep> createSteps(String[] actions, String[] expectedResults) {
        try {
            checkArraysToBeSameSize(actions, expectedResults);
        } catch (TestLinkAPIException e) {
            LOGGER.log(Level.SEVERE, "Error in test steps annotation", e);
        }

        // Create the list where to store the data
        List<TestCaseStep> testLinkSteps = new ArrayList<TestCaseStep>();

        // Add each step (action + expected result)
        for (int i = 0; i < actions.length; i++) {
            String action = actions[i];
            String expectedResult = expectedResults[i];

            TestCaseStep testLinkStep = new TestCaseStep();
            testLinkStep.setNumber(i);
            testLinkStep.setActions(action);
            testLinkStep.setExpectedResults(expectedResult);
            testLinkStep.setExecutionType(ExecutionType.MANUAL);
            testLinkSteps.add(testLinkStep);

        }
        return testLinkSteps;
    }

    /**
     * Converts a list of test steps composed of couples action-expected result in a {@link TestCaseStep} instance for
     * the TestLink API.
     * 
     * @param steps the steps to be converted
     * @return the TestLink API representation for the test steps
     */
    /* package */ List<TestCaseStep> createSteps(String[][] testSteps) {
        List<TestCaseStep> testLinkSteps = new ArrayList<TestCaseStep>();

        // Add each step (action + expected result)
        for (int i = 0; i < testSteps.length; i++) {
            String action = testSteps[i][0];
            String expectedResult = testSteps[i][1];

            TestCaseStep testLinkStep = new TestCaseStep();
            testLinkStep.setNumber(i++);
            testLinkStep.setActions(action);
            testLinkStep.setExpectedResults(expectedResult);
            testLinkStep.setExecutionType(ExecutionType.MANUAL);
            testLinkSteps.add(testLinkStep);
        }
        return testLinkSteps;
    }

    /**
     * Create a test case on TestLink.
     * 
     * @param testCaseName The name of the test case
     * @param testSuiteId The ID of the test suite
     * @param testProjectId The ID of the test project
     * @param authorLogin The author login
     * @param summary A summary for the test case
     * @param steps The test execution steps (for manual testing)
     * @param preconditions Test preconditions description
     * @param importance The importance level (high, medium, low)
     * @param executionType The execution type of the test: manual or automated
     * @param order An integer for the test order
     * @param internalId An internal ID for the test
     * @param checkDuplicatedName Check if the test has a duplicate name
     * @param actionOnDuplicatedName An action in case of duplicate names
     * @return Test Case
     */
    /* package */ TestCase createTestCaseWithSteps(String testCaseName, Integer testSuiteId, Integer testProjectId,
            String authorLogin, String summary, List<TestCaseStep> steps, String preconditions,
            TestImportance importance, ExecutionType executionType, Integer order, Integer internalId,
            boolean checkDuplicatedName, ActionOnDuplicate actionOnDuplicatedName) {
        TestCase testCase = api.createTestCase(testCaseName, testSuiteId, testProjectId, authorLogin, summary, steps,
                preconditions, importance, executionType, order, internalId, checkDuplicatedName,
                actionOnDuplicatedName);

        return testCase;
    }

    /**
     * Upload an attachment to a test case execution.
     * 
     * <p>
     * Parameter must be a string in the following format: <code>c:\\temp\\image.jpg</code>
     * </p>
     * 
     * @param attachmentFile the file to be attached to the test execution
     * @param executionId the execution ID
     * @param title a title for the attachment
     * @param description a description for the attachment
     * @param fileName the file name for the attachment
     * @param fileType MIME file type. I.e: image/jpeg
     * @return Attachment
     */
    /* package */ Attachment uploadAttachment(File attachmentFile, Integer executionId, String title, String description,
            String fileName, String fileType) {
        String fileContent = null;

        try {
            byte[] byteArray = FileUtils.readFileToByteArray(attachmentFile);
            fileContent = new String(Base64.encodeBase64(byteArray));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error when trying to read an attachment to be added to a test case", e);
        }

        Attachment attachment = api.uploadExecutionAttachment(executionId, title, description, fileName, fileType,
                fileContent);

        return attachment;
    }

    /**
     * Create a new test project on TestLink.
     * 
     * @param testProjectName Test project name
     * @param testProjectPrefix Prefix (used for Test case ID)
     * @param notes Project description
     * @param enableRequirements Enable Requirements feature
     * @param enableTestPriority Enable Testing Priority
     * @param enableAutomation Enable Test Automation (API keys)
     * @param enableInventory Enable Inventory
     * @param isActive Active project
     * @param isPublic Public project
     * @return Test project
     */
    /* package */ TestProject createNewTestProject(String testProjectName, String testProjectPrefix, String notes,
            boolean enableRequirements, boolean enableTestPriority, boolean enableAutomation, boolean enableInventory,
            boolean isActive, boolean isPublic) {
        TestProject project = null;

        try {
            project = api.createTestProject(testProjectName, testProjectPrefix, notes, enableRequirements,
                    enableTestPriority, enableAutomation, enableInventory, isActive, isPublic);
        } catch (TestLinkAPIException e) {
            LOGGER.log(Level.SEVERE, "Error while trying to create a new test project on TestLink", e);
        }

        return project;

    }

    /**
     * Data checking: the two input arrays must be not null and have same size.
     * 
     * @param arrayA
     * @param arrayB
     * @throws TestLinkJavaAPI for invalid input fields
     */
    private void checkArraysToBeSameSize(String[] arrayA, String[] arrayB) throws TestLinkAPIException {
        if (arrayA == null || arrayB == null) {
            throw new TestLinkAPIException("Input can not be null");
        }

        if (arrayA.length != arrayB.length) {
            throw new TestLinkAPIException("Input must have same size");
        }

    }
}
