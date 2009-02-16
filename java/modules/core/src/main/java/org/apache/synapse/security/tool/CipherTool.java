/*
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.security.tool;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.security.definition.CipherInformation;
import org.apache.synapse.security.definition.IdentityKeyStoreInformation;
import org.apache.synapse.security.definition.TrustKeyStoreInformation;
import org.apache.synapse.security.wrappers.CipherWrapper;
import org.apache.synapse.security.wrappers.IdentityKeyStoreWrapper;
import org.apache.synapse.security.wrappers.TrustKeyStoreWrapper;

import java.io.*;
import java.security.Key;

/**
 * Tool for encrypting and decrypting.
 * Arguments and their meanings
 * <ul>
 * <li>keystore     If keys are in a store ,it's location
 * <li>storepass    Password for access keyStore
 * <li>keypass      To get private key
 * <li>alias        Alias to identify key owner
 * <li>storetype    Type of keyStore
 * <li>keyfile      If key is in a file
 * <li>opmode       encrypt or decrypt
 * <li>algorithm    encrypt or decrypt algorithm
 * <li>source       Either cipher or plain text as an in-lined form
 * <li>sourceFile   Source from a file
 * <li>outEncode    Currently base64
 * <li>inEncode     Currently base64
 * <ul>
 */
public class CipherTool {

    private static Log log = LogFactory.getLog(CipherTool.class);

    /* The argument name for KeyStore location */
    public final static String KEY_STORE = "keystore";
    /* The KeyStore type*/
    public final static String STORE_TYPE = "storetype";
    /* The argument name for password to access KeyStore*/
    public final static String STORE_PASS = "storepass";
    /* The argument name for password for access private key */
    public final static String KEY_PASS = "keypass";
    /* The alias to identify key owner */
    public final static String ALIAS = "alias";

    /* If the key is from a file , then it's location*/
    public final static String KEY_FILE = "keyfile";

    /* The algorithm for encrypting or decrypting */
    public final static String ALGORITHM = "algorithm";
    /* The operation mode of cihper - encrypt or decrypt */
    public final static String OP_MODE = "opmode";
    /* The cipher type - asymmetric , symmetric */
    public final static String CIPHER_TYPE = "ciphertype";

    /* The cipher or plain text as an in-lined */
    public final static String SOURCE_IN_LINED = "source";
    /* The the source from a file*/
    public final static String SOURCE_FILE = "sourcefile";
    /* If  the target has to be written to a file*/
    public final static String TARGET_FILE = "targetfile";
    /* If  the output of cipher operation need to be encode - only base64*/
    public final static String OUT_TYPE = "outtype";
    /* If  the encode of the input type base64*/
    public final static String IN_TYPE = "intype";
    /* Is this keyStore a trusted one */
    public final static String TRUSTED = "trusted";

    /* Operation mode */
    public final static String ENCRYPT = "encrypt";
    public final static String DECRYPT = "decrypt";

    public final static String BASE64 = "base64";

    public static void main(String args[]) throws Exception {

        // loads the options
        Options options = getOptions();
        // create the command line parser
        CommandLineParser parser = new GnuParser();
        // parse the command line arguments
        try {
            CommandLine cmd = parser.parse(options, args);
            // Loads the cipher relate information
            CipherInformation cipherInformation = getCipherInformation(cmd);
            //Key information must not contain any password
            //Password for access private key
            String keyPass = getArgument(cmd, KEY_PASS);
            // If Key need to be loaded from a file
            String keyFile = getArgument(cmd, KEY_FILE);
            // Source  as an in-lined
            String source = getArgument(cmd, SOURCE_IN_LINED);

            boolean isTrusted = isArgumentPresent(cmd, TRUSTED);

            Key key;
            if (keyFile != null) {
                key = getKey(keyFile);
            } else {
                if (isTrusted) {
                    TrustKeyStoreWrapper trustKeyStoreWrapper = new TrustKeyStoreWrapper();
                    trustKeyStoreWrapper.init(getTrustKeyStoreInformation(cmd));
                    key = trustKeyStoreWrapper.getPublicKey();
                } else {
                    IdentityKeyStoreWrapper storeWrapper = new IdentityKeyStoreWrapper();
                    storeWrapper.init(getIdentityKeyStoreInformation(cmd), keyPass);
                    if (ENCRYPT.equals(cipherInformation.getOperationMode())) {
                        key = storeWrapper.getPrivateKey();
                    } else {
                        key = storeWrapper.getPublicKey();
                    }
                }
            }

            if (key == null) {
                handleException("Cannot find a key ");
            }

            CipherWrapper cipherWrapper = new CipherWrapper(cipherInformation, key);
            ByteArrayInputStream in = new ByteArrayInputStream(source.getBytes());

            System.out.println("Output : " + cipherWrapper.getSecret(in));

        } catch (ParseException e) {
            handleException("Error passing arguments ", e);
        }
    }

    /**
     * Utility method to extract command line arguments
     *
     * @param cmd     Command line which capture all command line arguments
     * @param argName Name of the argument to be extracted
     * @return value of the argument if there is , o.w null
     */
    private static String getArgument(CommandLine cmd, String argName) {

        if (cmd == null) {
            handleException("CommandLine is null");
        }

        if (argName == null || "".equals(argName)) {
            if (log.isDebugEnabled()) {
                log.debug("Provided argument name is null. Returning null as value");
            }
            return null;
        }

        if (cmd.hasOption(argName)) {
            return cmd.getOptionValue(argName);
        }
        return null;
    }

    /**
     * Utility method to find boolean argument
     *
     * @param cmd     Command line which capture all command line arguments
     * @param argName Name of the argument to be extracted
     * @return True if presents
     */
    private static boolean isArgumentPresent(CommandLine cmd, String argName) {
        if (cmd == null) {
            handleException("CommandLine is null");
        }

        if (argName == null || "".equals(argName)) {
            if (log.isDebugEnabled()) {
                log.debug("Provided argument name is null. Returning null as value");
            }
            return false;
        }

        return cmd.hasOption(argName);
    }

    /**
     * Factory method to construct @see CipherInformation from command line options
     *
     * @param cmd Command line which capture all command line arguments
     * @return CipherInformation object
     */
    private static CipherInformation getCipherInformation(CommandLine cmd) {

        CipherInformation information = new CipherInformation();
        information.setAlgorithm(getArgument(cmd, ALGORITHM));
        information.setOperationMode(getArgument(cmd, OP_MODE));
        information.setInType(getArgument(cmd, IN_TYPE));
        information.setOutType(getArgument(cmd, OUT_TYPE));
        information.setType(getArgument(cmd, CIPHER_TYPE));
        return information;

    }

    /**
     * Factory method to create a @see keyStoreInformation from command line options
     *
     * @param cmd Command line which capture all command line arguments
     * @return KeyStoreInformation object
     */
    private static IdentityKeyStoreInformation getIdentityKeyStoreInformation(CommandLine cmd) {

        IdentityKeyStoreInformation information = new IdentityKeyStoreInformation();
        information.setAlias(getArgument(cmd, ALIAS));
        information.setLocation(getArgument(cmd, KEY_STORE));
        information.setStoreType(getArgument(cmd, STORE_TYPE));
        information.setKeyStorePassword(getArgument(cmd, STORE_PASS));
        return information;

    }

    /**
     * Factory method to create a @see keyStoreInformation from command line options
     *
     * @param cmd Command line which capture all command line arguments
     * @return KeyStoreInformation object
     */
    private static TrustKeyStoreInformation getTrustKeyStoreInformation(CommandLine cmd) {

        TrustKeyStoreInformation information = new TrustKeyStoreInformation();
        information.setAlias(getArgument(cmd, ALIAS));
        information.setLocation(getArgument(cmd, KEY_STORE));
        information.setStoreType(getArgument(cmd, STORE_TYPE));
        information.setKeyStorePassword(getArgument(cmd, STORE_PASS));
        return information;

    }

    /**
     * Factory method to create options
     *
     * @return Options object
     */
    private static Options getOptions() {

        Options options = new Options();

        Option keyStore = new Option(KEY_STORE, true, "Private key entry KeyStore");
        Option storeType = new Option(STORE_TYPE, true, " KeyStore type");
        Option storePassword = new Option(STORE_PASS, true, "Password for keyStore access");
        Option keyPassword = new Option(KEY_PASS, true, "Password for access private key entry");
        Option alias = new Option(ALIAS, true, "Alias name for identify key owner");
        Option trusted = new Option(TRUSTED, false, "Is this KeyStore trusted one");

        Option keyFile = new Option(KEY_FILE, true, "Private key from a file");
        Option cipherType = new Option(CIPHER_TYPE, true, "Cipher type");
        Option opMode = new Option(OP_MODE, true, "encrypt or decrypt");

        Option algorithm = new Option(ALGORITHM, true, "Algorithm to be used");
        Option source = new Option(SOURCE_IN_LINED, true, "Plain text in-lined");
        Option sourceFile = new Option(SOURCE_FILE, true, "Plain text from a file");
        Option targetFile = new Option(TARGET_FILE, true, "Target file");
        Option outType = new Option(OUT_TYPE, true, "Encode type for output");
        Option intType = new Option(IN_TYPE, true, "Encode type of input source");

        options.addOption(keyStore);
        options.addOption(storeType);
        options.addOption(storePassword);
        options.addOption(keyPassword);
        options.addOption(alias);
        options.addOption(trusted);

        options.addOption(keyFile);

        options.addOption(algorithm);
        options.addOption(cipherType);
        options.addOption(opMode);

        options.addOption(source);
        options.addOption(sourceFile);
        options.addOption(targetFile);
        options.addOption(outType);
        options.addOption(intType);

        return options;
    }

    /**
     * Factory method to retrieve a previously stored key in a file
     *
     * @param filePath Path to file
     * @return Retrieved key
     */
    private static Key getKey(String filePath) {

        if (filePath == null || "".equals(filePath)) {
            handleException("File path cannot be empty or null");
        }

        File keyFile = new File(filePath);
        if (!keyFile.exists()) {
            handleException("File cannot be found in : " + filePath);
        }

        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(keyFile));
            Object object = in.readObject();
            if (object instanceof Key) {
                return (Key) object;
            }

        } catch (IOException e) {
            handleException("Error reading key from given path : " + filePath, e);
        } catch (ClassNotFoundException e) {
            handleException("Cannot load a key from the file : " + filePath, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
