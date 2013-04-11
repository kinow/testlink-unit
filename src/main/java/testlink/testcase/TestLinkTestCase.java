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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import testlink.annotations.Coverage;
import testlink.annotations.TestInfo;
import testlink.annotations.TestScript;
import br.eti.kinoshita.testlinkjavaapi.constants.ActionOnDuplicate;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionType;
import br.eti.kinoshita.testlinkjavaapi.constants.TestImportance;
import br.eti.kinoshita.testlinkjavaapi.model.Requirement;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestCaseStep;
import br.eti.kinoshita.testlinkjavaapi.model.TestProject;
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite;

/**
 * This abstract class provides the handling of test annotations for TestLink.
 * 
 * <p>
 * Implementing ISO/IEC 29119 specification.
 * </p>
 * 
 * <p>
 * Classes extending this class must call the constructor passing values that are used to connect with TestLink.
 * Problems in this connection will raise exceptions in the <code>setUp</code> method call.
 * </p>
 * 
 * @author mcaste00
 * @since 0.1
 */
public abstract class TestLinkTestCase extends junit.framework.TestCase {

    private static final Logger LOGGER = Logger.getLogger(TestLinkTestCase.class.getName());

    private TestLinkSite testlink;

    /**
     * Estabilishes connection with TestLink.
     * 
     * @param url TestLink URL
     * @param devKey TestLink developer key
     * @throws RuntimeException if it is not able to connect to TestLink
     */
    protected void connect(String url, String devKey) {
        // Open connection to TestLink
        testlink = new TestLinkSite(url, devKey);
    }

    @Before
    public void setUp() {

        final String url = System.getProperty("testlink.url");
        final String devKey = System.getProperty("testlink.devkey");

        if (StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(devKey)) {
            LOGGER.log(Level.INFO, "Connecting to TestLink");
            try {
                // online
                this.connect(url, devKey);
    
                // Fetch annotations: test-case information
                final TestInfo testInfo = getAnnotation(TestInfo.class);
                String testProjectName = testInfo.project();
                String testSuiteName = testInfo.suite();
    
                // Fetch annotations: requirements
                final Coverage reqAnnotation = getAnnotation(Coverage.class);
                int srs = new Integer(reqAnnotation.srs()).intValue();
                String[] requirements = reqAnnotation.requirements();
    
                // Fetch annotations: test steps
                String[] actions = getAnnotation(TestScript.class).actions();
                String[] expectedResults = getAnnotation(TestScript.class).expectedResults();
                List<TestCaseStep> testSteps = testlink.createSteps(actions, expectedResults);
    
                // Get TestLink references: test project and suite
                final TestProject testProject = testlink.getTestProject(testProjectName);
                if (testProject == null) {
                    throw new RuntimeException("Could not find test project: " + testProjectName);
                }
                
                final TestSuite testSuite = testlink.getTestSuite(testProject.getId(), testSuiteName);
                
                if (testSuite == null) {
                    throw new RuntimeException("Could not find test suite: " + testSuiteName);
                }
    
                // Create the test case
                final TestCase testCase = testlink.createTestCaseWithSteps(
                        this.getClass().getCanonicalName(),// Test Case Name
                        testSuite.getId(), 
                        testProject.getId(), 
                        System.getProperty("testlink.author", "admin"), 
                        System.getProperty("testlink.summary", "Exported Unit Test"), 
                        testSteps, 
                        System.getProperty("testlink.preconditions", "No preconditions for this test"),
                        TestImportance.MEDIUM, 
                        ExecutionType.AUTOMATED, 
                        null,// Order
                        null,// Internal ID
                        true,// Check Duplicated Names
                        ActionOnDuplicate.CREATE_NEW_VERSION);// Replace old with new
    
                // Add requirements to the test case
                setRequirements(testCase, srs, requirements);
    
                /* TODO: FROM HERE ON IS EXPERIMENTAL */
                   
                /*
                 * final String CUSTOM_FIELD = "Java Class";
            final String AUTHOR_LOGIN = "admin";
            final String TEST_PRECONDITIONS = "No preconditions for this test";
                 */
                //
                // // Add the class name in the Java Class field
                //
                // // Get custom field
                // CustomField customField = getCustomField(
                // testProject.getId(),
                // testCase.getId(),
                // CUSTOM_FIELD,
                // new Integer(29));// Is it possible to automatically have the last version?
                //
                // customField.setValue("just-a-test");// This is the value we want in the custom field
                //
                // List<CustomField> customFields = new ArrayList<CustomField>();// Needed as testCase.getCustomFields() is
                // null
                // customFields.add(customField);
                // testCase.setCustomFields(customFields);// Seems not to work : custom field is not updated
                //
                // log.debug("Custom fields: [" + customFields + "]");
                // log.debug("Custom fields size: [" + customFields.size() + "]");
            } catch (RuntimeException re) {
                LOGGER.log(Level.SEVERE, "Error running test: " + re.getMessage(), re);
                throw re;
            }
        } else {
            LOGGER.log(Level.INFO, "Running test offline");
        }
    }

    /**
     * Given a list of requirements being part of an SRS folder, links them to the test case.
     * 
     * @param testCase the test case to be linked to requirements
     * @param srsId the SRS folder ID
     * @param requirementsId an array of requirement IDs
     */
    protected void setRequirements(TestCase testCase, int srsId, String[] requirementsId) {

        // Prepare the data
        List<Requirement> requirements = new ArrayList<Requirement>();

        // Loop the requirements to be added
        for (String requirementId : requirementsId) {

            int reqIdNumber = Integer.parseInt(requirementId);

            Requirement requirement = new Requirement();
            requirement.setId(reqIdNumber);
            // TBD: requirement.setReqDocId(requirementId);
            requirement.setReqSpecId(srsId);
            requirements.add(requirement);
        }

        // Link the requirements to the test case
        testlink.assignRequirements(testCase, requirements);
    }

    /**
     * Conveniency method to get an {@link Annotation} object instantiated on the currently running test
     * 
     * @param <A> the annotation type
     * @param type the annotation object
     * @return the annotation instantiated by the current test
     */
    protected <A extends Annotation> A getAnnotation(Class<A> type) {
        A annotation = this.getClass().getAnnotation(type);
        if (annotation == null) {
            LOGGER.log(Level.SEVERE, "Error: annotation object is null!");
        }
        return annotation;
    }
}
