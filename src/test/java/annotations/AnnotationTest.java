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
package annotations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import testlink.annotations.Coverage;
import testlink.annotations.TestInfo;
import testlink.annotations.TestScript;
import testlink.testcase.TestLinkTestCase;

/**
 * An example usage of annotations for tests requirements
 * 
 * @author mcaste00
 * @since 0.1
 */
@TestInfo(project = "p1", suite = "s1")
@Coverage(srs = "175", requirements = {  }) // TBD: use requirement
@TestScript(actions = { "1. Open application", "2. Login", "3. Click exit button" }, expectedResults = {
        "1. Application starts", "2. User is authenticated", "3. Application closes" })
public class AnnotationTest extends TestLinkTestCase {

    private static final Logger LOGGER = Logger.getLogger(AnnotationTest.class.getName());
    
    static {
        //set the console handler to fine:
        System.setProperty("testlink.url", "http://localhost/testlink-1.9.6/lib/api/xmlrpc.php");
        System.setProperty("testlink.devkey", "57f7cf6a3319d271bb83bbf378ef1e6e");
    }
    
    @Test
    public void testNothing() {
        LOGGER.log(Level.FINEST, "Simple test");
        LOGGER.info("Entered the test");
        assertTrue("I am working", true);
    }
}
