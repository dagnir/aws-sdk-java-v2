/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.internal.http.apache.client.impl.ApacheHttpClientFactory;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;


public class TTE004146470IntegrationTest {
    //        Subject: C=US, ST=California, L=Mountain View, O=Google Inc, CN=*.google.com
    //        Issuer: C=US, O=Google Inc, CN=Google Internet Authority G2
    static final String real_star_google_com = "-----BEGIN CERTIFICATE-----\n"
                                               + "MIIHgzCCBmugAwIBAgIIacinF0JVV4IwDQYJKoZIhvcNAQEFBQAwSTELMAkGA1UEBhMCVVMxEzAR\n"
                                               + "BgNVBAoTCkdvb2dsZSBJbmMxJTAjBgNVBAMTHEdvb2dsZSBJbnRlcm5ldCBBdXRob3JpdHkgRzIw\n"
                                               + "HhcNMTQxMDIyMTI1NTEwWhcNMTUwMTIwMDAwMDAwWjBmMQswCQYDVQQGEwJVUzETMBEGA1UECAwK\n"
                                               + "Q2FsaWZvcm5pYTEWMBQGA1UEBwwNTW91bnRhaW4gVmlldzETMBEGA1UECgwKR29vZ2xlIEluYzEV\n"
                                               + "MBMGA1UEAwwMKi5nb29nbGUuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2rjr\n"
                                               + "nV+6+kUD527ybMkhqLNhfcRlRRfJmMS4UVB+cr9QlcvKwAG5zEQ8b/Ft1m1RFvUWhRPbM2rjWnca\n"
                                               + "BcHGh4o+gALs4CORLLA4OFSkST3E9A/hDlSQl5yV8RJrNxNwgtaRTOIHDBx9fAnhT2POA7UP/iUw\n"
                                               + "whiTu2QFNZC7WYCXd2CP1phmWNivh4vk1O+NiIORAcvFjeLDDLKXTqgZ5vR3EQR5uLfhb3qTfUz2\n"
                                               + "IGoeWP18mF7SchsxCGDriopbWS3E4Mbgbl+CTSlVmaZRWyBGkr7DIueQBEw858sp4uJeO9kOHpKe\n"
                                               + "4zisrj5IXWxG46ShjhGO3xabxSkR0K+8AwIDAQABo4IEUDCCBEwwHQYDVR0lBBYwFAYIKwYBBQUH\n"
                                               + "AwEGCCsGAQUFBwMCMIIDJgYDVR0RBIIDHTCCAxmCDCouZ29vZ2xlLmNvbYINKi5hbmRyb2lkLmNv\n"
                                               + "bYIWKi5hcHBlbmdpbmUuZ29vZ2xlLmNvbYISKi5jbG91ZC5nb29nbGUuY29tghYqLmdvb2dsZS1h\n"
                                               + "bmFseXRpY3MuY29tggsqLmdvb2dsZS5jYYILKi5nb29nbGUuY2yCDiouZ29vZ2xlLmNvLmlugg4q\n"
                                               + "Lmdvb2dsZS5jby5qcIIOKi5nb29nbGUuY28udWuCDyouZ29vZ2xlLmNvbS5hcoIPKi5nb29nbGUu\n"
                                               + "Y29tLmF1gg8qLmdvb2dsZS5jb20uYnKCDyouZ29vZ2xlLmNvbS5jb4IPKi5nb29nbGUuY29tLm14\n"
                                               + "gg8qLmdvb2dsZS5jb20udHKCDyouZ29vZ2xlLmNvbS52boILKi5nb29nbGUuZGWCCyouZ29vZ2xl\n"
                                               + "LmVzggsqLmdvb2dsZS5mcoILKi5nb29nbGUuaHWCCyouZ29vZ2xlLml0ggsqLmdvb2dsZS5ubIIL\n"
                                               + "Ki5nb29nbGUucGyCCyouZ29vZ2xlLnB0ghIqLmdvb2dsZWFkYXBpcy5jb22CDyouZ29vZ2xlYXBp\n"
                                               + "cy5jboIUKi5nb29nbGVjb21tZXJjZS5jb22CESouZ29vZ2xldmlkZW8uY29tggwqLmdzdGF0aWMu\n"
                                               + "Y26CDSouZ3N0YXRpYy5jb22CCiouZ3Z0MS5jb22CCiouZ3Z0Mi5jb22CFCoubWV0cmljLmdzdGF0\n"
                                               + "aWMuY29tggwqLnVyY2hpbi5jb22CECoudXJsLmdvb2dsZS5jb22CFioueW91dHViZS1ub2Nvb2tp\n"
                                               + "ZS5jb22CDSoueW91dHViZS5jb22CFioueW91dHViZWVkdWNhdGlvbi5jb22CCyoueXRpbWcuY29t\n"
                                               + "ggthbmRyb2lkLmNvbYIEZy5jb4IGZ29vLmdsghRnb29nbGUtYW5hbHl0aWNzLmNvbYIKZ29vZ2xl\n"
                                               + "LmNvbYISZ29vZ2xlY29tbWVyY2UuY29tggp1cmNoaW4uY29tggh5b3V0dS5iZYILeW91dHViZS5j\n"
                                               + "b22CFHlvdXR1YmVlZHVjYXRpb24uY29tMGgGCCsGAQUFBwEBBFwwWjArBggrBgEFBQcwAoYfaHR0\n"
                                               + "cDovL3BraS5nb29nbGUuY29tL0dJQUcyLmNydDArBggrBgEFBQcwAYYfaHR0cDovL2NsaWVudHMx\n"
                                               + "Lmdvb2dsZS5jb20vb2NzcDAdBgNVHQ4EFgQUBuDrUZa0gBZaOoUMD3Dq2248I+gwDAYDVR0TAQH/\n"
                                               + "BAIwADAfBgNVHSMEGDAWgBRK3QYWG7z2aLV29YG2u2IaulqBLzAXBgNVHSAEEDAOMAwGCisGAQQB\n"
                                               + "1nkCBQEwMAYDVR0fBCkwJzAloCOgIYYfaHR0cDovL3BraS5nb29nbGUuY29tL0dJQUcyLmNybDAN\n"
                                               + "BgkqhkiG9w0BAQUFAAOCAQEAQZMjMzxhMYAtz92XU55PSQAJ41QZYxeAvW7SYPGmH+Q/b3ubvdlU\n"
                                               + "r4oSTAHkCotDd0xwc/SA9wCLHLW0nDI+f1Mzd9bkXNrYX/CLygCIK9XasxxF9p+29DN9zpeBTjCB\n"
                                               + "aiGCBE8tPrxvGODFpPR+T8FT0f2bpBlpKg//reMvTM8EDVu3cFmTKlwQ+Th17zvh0LSnLoBzuKLT\n"
                                               + "l9EFZFSdgm2xM1EGXrWa5vPp6+oYLzRA/3Wb02XY6v6vPXTewk8UQSJQpxSwCtczcrvqslKjlFrQ\n"
                                               + "0BX6Bb2akWk+Mps4KSswMNUU1p+3ksA/7I23R5P9bnhDrs23ZezRcVQnZeu7BA==\n"
                                               + "-----END CERTIFICATE-----\n";
    //         Subject: C=US, O=Google Inc, CN=Google Internet Authority G2
    //         Issuer: C=US, O=GeoTrust Inc., CN=GeoTrust Global CA
    static final String real_google_g2 = "-----BEGIN CERTIFICATE-----\n"
                                         + "MIID8DCCAtigAwIBAgIDAjp2MA0GCSqGSIb3DQEBBQUAMEIxCzAJBgNVBAYTAlVTMRYwFAYDVQQK\n"
                                         + "Ew1HZW9UcnVzdCBJbmMuMRswGQYDVQQDExJHZW9UcnVzdCBHbG9iYWwgQ0EwHhcNMTMwNDA1MTUx\n"
                                         + "NTU1WhcNMTYxMjMxMjM1OTU5WjBJMQswCQYDVQQGEwJVUzETMBEGA1UEChMKR29vZ2xlIEluYzEl\n"
                                         + "MCMGA1UEAxMcR29vZ2xlIEludGVybmV0IEF1dGhvcml0eSBHMjCCASIwDQYJKoZIhvcNAQEBBQAD\n"
                                         + "ggEPADCCAQoCggEBAJwqBHdc2FCROgajguDYUEi8iT/xGXAaiEZ+4I/F8YnOIe5a/mENtzJEiaB0\n"
                                         + "C1NPVaTOgmKV7utZX8bhBYASxF6UP7xbSDj0U/ck5vuR6RXEz/RTDfRK/J9U3n2+oGtvh8DQUB8o\n"
                                         + "MANA2ghzUWx//zo8pzcGjr1LEQTrfSTe5vn8MXH7lNVg8y5Kr0LSy+rEahqyzFPdFUuLH8gZYR/N\n"
                                         + "nag+YyuENWllhMgZxUYi+FOVvuOAShDGKuy6lyARxzmZEASg8GF6lSWMTlJ14rbtCMoU/M4iarNO\n"
                                         + "z0YDl5cDfsCx3nuvRTPPuj5xt970JSXCDTWJnZ37DhF5iR43xa+OcmkCAwEAAaOB5zCB5DAfBgNV\n"
                                         + "HSMEGDAWgBTAephojYn7qwVkDBF9qn1luMrMTjAdBgNVHQ4EFgQUSt0GFhu89mi1dvWBtrtiGrpa\n"
                                         + "gS8wEgYDVR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAQYwNQYDVR0fBC4wLDAqoCigJoYk\n"
                                         + "aHR0cDovL2cuc3ltY2IuY29tL2NybHMvZ3RnbG9iYWwuY3JsMC4GCCsGAQUFBwEBBCIwIDAeBggr\n"
                                         + "BgEFBQcwAYYSaHR0cDovL2cuc3ltY2QuY29tMBcGA1UdIAQQMA4wDAYKKwYBBAHWeQIFATANBgkq\n"
                                         + "hkiG9w0BAQUFAAOCAQEAJ4zP6cc7vsBv6JaE+5xcXZDkd9uLMmCbZdiFJrW6nx7eZE4fxsggWwmf\n"
                                         + "q6ngCTRFomUlNz1/Wm8gzPn68R2PEAwCOsTJAXaWvpv5Fdg50cUDR3a4iowx1mDV5I/b+jzG1Zgo\n"
                                         + "+ByPF5E0y8tSetH7OiDk4Yax2BgPvtaHZI3FCiVCUe+yOLjgHdDh/Ob0r0a678C/xbQF9ZR1DP6i\n"
                                         + "vgK66oZb+TWzZvXFjYWhGiN3GhkXVBNgnwvhtJwoKvmuAjRtJZOcgqgXe/GFsNMPWOH7sf6coaPo\n"
                                         + "/ck/9Ndx3L2MpBngISMjVROPpBYCCX65r+7bU2S9cS+5Oc4wt7S8VOBHBw==\n"
                                         + "-----END CERTIFICATE-----\n";
    //        Subject: C=US, O=GeoTrust Inc., CN=GeoTrust Global CA
    //        Issuer: C=US, O=Equifax, OU=Equifax Secure Certificate Authority
    static String real_geo_trust = "-----BEGIN CERTIFICATE-----\n"
                                   + "MIIDfTCCAuagAwIBAgIDErvmMA0GCSqGSIb3DQEBBQUAME4xCzAJBgNVBAYTAlVTMRAwDgYDVQQK\n"
                                   + "EwdFcXVpZmF4MS0wKwYDVQQLEyRFcXVpZmF4IFNlY3VyZSBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkw\n"
                                   + "HhcNMDIwNTIxMDQwMDAwWhcNMTgwODIxMDQwMDAwWjBCMQswCQYDVQQGEwJVUzEWMBQGA1UEChMN\n"
                                   + "R2VvVHJ1c3QgSW5jLjEbMBkGA1UEAxMSR2VvVHJ1c3QgR2xvYmFsIENBMIIBIjANBgkqhkiG9w0B\n"
                                   + "AQEFAAOCAQ8AMIIBCgKCAQEA2swYYzD99BcjGlZ+W988bDjkcbd4kdS8odhM+KhDtgPpTSEHCIja\n"
                                   + "WC9mOSm9BXiLnTjoBbdqfnGk5sRgprDvgOSJKA+eJdbtg/OtppHHmMlCGDUUna2YRpIuT8rxh0PB\n"
                                   + "FpVXLVDviS2Aelet8u5fa9IAjbkU+BQVNdnARqN7csiRv8lVK83Qlz6cJmTM386DGXHKTubU1Xup\n"
                                   + "Gc1V3sjs0l44U+VcT4wt/lAjNvxm5suOpDkZALeVAjmRCw7+OC7RHQWa9k0+bw8HHa8sHo9gOeL6\n"
                                   + "NlMTOdReJivbPagUvTLrGAMoUgRx5aszPeE4uwc2hGKceeoWMPRfwCvocWvk+QIDAQABo4HwMIHt\n"
                                   + "MB8GA1UdIwQYMBaAFEjmaPkr0rKV10fYIyAQTzOYkJ/UMB0GA1UdDgQWBBTAephojYn7qwVkDBF9\n"
                                   + "qn1luMrMTjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjA6BgNVHR8EMzAxMC+gLaAr\n"
                                   + "hilodHRwOi8vY3JsLmdlb3RydXN0LmNvbS9jcmxzL3NlY3VyZWNhLmNybDBOBgNVHSAERzBFMEMG\n"
                                   + "BFUdIAAwOzA5BggrBgEFBQcCARYtaHR0cHM6Ly93d3cuZ2VvdHJ1c3QuY29tL3Jlc291cmNlcy9y\n"
                                   + "ZXBvc2l0b3J5MA0GCSqGSIb3DQEBBQUAA4GBAHbhEm5OSxYShjAGsoEIz/AIx8dxfmbuwu3UOx//\n"
                                   + "8PDITtZDOLC5MH0Y0FWDomrLNhGc6Ehmo21/uBPUR/6LWlxz/K7ZGzIZOKuXNBSqltLroxwUCEm2\n"
                                   + "u+WR74M26x1Wb8ravHNjkOR/ez4iyz0H7V84dJzjA1BOoa+Y7mHyhD8S\n"
                                   + "-----END CERTIFICATE-----\n";
    //        Subject: C=US, ST=California, L=Mountain View, O=Google Inc, CN=*.google.com
    //        Issuer: C=US, O=Google Inc, CN=Google Internet Authority G2
    String cert1_googleBogus = "-----BEGIN CERTIFICATE-----\n"
                               + "MIIGxTCCBa2gAwIBAgIIZI1RinFmQYYwDQYJKoZIhvcNAQEFBQAwSTELMAkGA1UEBhMCVVMxEzAR\n"
                               + "BgNVBAoTCkdvb2dsZSBJbmMxJTAjBgNVBAMTHEdvb2dsZSBJbnRlcm5ldCBBdXRob3JpdHkgRzIw\n"
                               + "HhcNMTQxMDA4MTIwMTA4WhcNMTUwMTA2MDAwMDAwWjBmMQswCQYDVQQGEwJVUzETMBEGA1UECAwK\n"
                               + "Q2FsaWZvcm5pYTEWMBQGA1UEBwwNTW91bnRhaW4gVmlldzETMBEGA1UECgwKR29vZ2xlIEluYzEV\n"
                               + "MBMGA1UEAwwMKi5nb29nbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEq2J/zPbENhK0\n"
                               + "ztmeJRg8XnlJ+gh4o7nUab1vyFbSR90VU3B+P9pcCfA1KzzJ6BYH1Zs1XP+Yo7j8NPhnCHXslaOC\n"
                               + "BF0wggRZMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjCCAyYGA1UdEQSCAx0wggMZggwq\n"
                               + "Lmdvb2dsZS5jb22CDSouYW5kcm9pZC5jb22CFiouYXBwZW5naW5lLmdvb2dsZS5jb22CEiouY2xv\n"
                               + "dWQuZ29vZ2xlLmNvbYIWKi5nb29nbGUtYW5hbHl0aWNzLmNvbYILKi5nb29nbGUuY2GCCyouZ29v\n"
                               + "Z2xlLmNsgg4qLmdvb2dsZS5jby5pboIOKi5nb29nbGUuY28uanCCDiouZ29vZ2xlLmNvLnVrgg8q\n"
                               + "Lmdvb2dsZS5jb20uYXKCDyouZ29vZ2xlLmNvbS5hdYIPKi5nb29nbGUuY29tLmJygg8qLmdvb2ds\n"
                               + "ZS5jb20uY2+CDyouZ29vZ2xlLmNvbS5teIIPKi5nb29nbGUuY29tLnRygg8qLmdvb2dsZS5jb20u\n"
                               + "dm6CCyouZ29vZ2xlLmRlggsqLmdvb2dsZS5lc4ILKi5nb29nbGUuZnKCCyouZ29vZ2xlLmh1ggsq\n"
                               + "Lmdvb2dsZS5pdIILKi5nb29nbGUubmyCCyouZ29vZ2xlLnBsggsqLmdvb2dsZS5wdIISKi5nb29n\n"
                               + "bGVhZGFwaXMuY29tgg8qLmdvb2dsZWFwaXMuY26CFCouZ29vZ2xlY29tbWVyY2UuY29tghEqLmdv\n"
                               + "b2dsZXZpZGVvLmNvbYIMKi5nc3RhdGljLmNugg0qLmdzdGF0aWMuY29tggoqLmd2dDEuY29tggoq\n"
                               + "Lmd2dDIuY29tghQqLm1ldHJpYy5nc3RhdGljLmNvbYIMKi51cmNoaW4uY29tghAqLnVybC5nb29n\n"
                               + "bGUuY29tghYqLnlvdXR1YmUtbm9jb29raWUuY29tgg0qLnlvdXR1YmUuY29tghYqLnlvdXR1YmVl\n"
                               + "ZHVjYXRpb24uY29tggsqLnl0aW1nLmNvbYILYW5kcm9pZC5jb22CBGcuY2+CBmdvby5nbIIUZ29v\n"
                               + "Z2xlLWFuYWx5dGljcy5jb22CCmdvb2dsZS5jb22CEmdvb2dsZWNvbW1lcmNlLmNvbYIKdXJjaGlu\n"
                               + "LmNvbYIIeW91dHUuYmWCC3lvdXR1YmUuY29tghR5b3V0dWJlZWR1Y2F0aW9uLmNvbTALBgNVHQ8E\n"
                               + "BAMCB4AwaAYIKwYBBQUHAQEEXDBaMCsGCCsGAQUFBzAChh9odHRwOi8vcGtpLmdvb2dsZS5jb20v\n"
                               + "R0lBRzIuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vY2xpZW50czEuZ29vZ2xlLmNvbS9vY3NwMB0G\n"
                               + "A1UdDgQWBBSEsiuGgIQy3N+UF5yTlAWbcAFoJjAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFErd\n"
                               + "BhYbvPZotXb1gba7Yhq6WoEvMBcGA1UdIAQQMA4wDAYKKwYBBAHWeQIFATAwBgNVHR8EKTAnMCWg\n"
                               + "I6Ahhh9odHRwOi8vcGtpLmdvb2dsZS5jb20vR0lBRzIuY3JsMA0GCSqGSIb3DQEBBQUAA4IBAQBQ\n"
                               + "BwdtJvXBLtpRPgp6d+8+pG5y8NkjW6n1D3nKtoy4TejWrPQEZZMvxZRJtBXZcnjGc97Pl9kJAAJ2\n"
                               + "CbvCwHG2oURHdgCxd6X07HmEam36erzjHrW9J+IYT1NMYFtshw2e7HqepZeKrMSP8bnRhG8+a2+6\n"
                               + "ZmQ7/GuE/B2jng+1fg4t1TBxGng6v9sPc7Q1+2cHw5Me5lwPwaRBJ4To2ndVLpjhNAMWPvM5Fanp\n"
                               + "aWlc6HkKk+fDyMfLD+jV5K+TkdbQSXzNj+OAt+mxyrYifhO+a5L8AKtslCXYi6cVTJwUXvfKJEs1\n"
                               + "LWGnRWSzEFaWttvNAw+AsKztOInXX05dm3Jk\n"
                               + "-----END CERTIFICATE-----";
    //        Subject: C=US, O=GeoTrust Inc., CN=GeoTrust Global CA
    //        Issuer: C=US, O=GeoTrust Inc., CN=GeoTrust Global CA
    String cert2_geoTrust_suspect = "-----BEGIN CERTIFICATE-----\n"
                                    + "MIIDVDCCAjygAwIBAgIDAjRWMA0GCSqGSIb3DQEBBQUAMEIxCzAJBgNVBAYTAlVTMRYwFAYDVQQK\n"
                                    + "Ew1HZW9UcnVzdCBJbmMuMRswGQYDVQQDExJHZW9UcnVzdCBHbG9iYWwgQ0EwHhcNMDIwNTIxMDQw\n"
                                    + "MDAwWhcNMjIwNTIxMDQwMDAwWjBCMQswCQYDVQQGEwJVUzEWMBQGA1UEChMNR2VvVHJ1c3QgSW5j\n"
                                    + "LjEbMBkGA1UEAxMSR2VvVHJ1c3QgR2xvYmFsIENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\n"
                                    + "CgKCAQEA2swYYzD99BcjGlZ+W988bDjkcbd4kdS8odhM+KhDtgPpTSEHCIjaWC9mOSm9BXiLnTjo\n"
                                    + "BbdqfnGk5sRgprDvgOSJKA+eJdbtg/OtppHHmMlCGDUUna2YRpIuT8rxh0PBFpVXLVDviS2Aelet\n"
                                    + "8u5fa9IAjbkU+BQVNdnARqN7csiRv8lVK83Qlz6cJmTM386DGXHKTubU1XupGc1V3sjs0l44U+Vc\n"
                                    + "T4wt/lAjNvxm5suOpDkZALeVAjmRCw7+OC7RHQWa9k0+bw8HHa8sHo9gOeL6NlMTOdReJivbPagU\n"
                                    + "vTLrGAMoUgRx5aszPeE4uwc2hGKceeoWMPRfwCvocWvk+QIDAQABo1MwUTAPBgNVHRMBAf8EBTAD\n"
                                    + "AQH/MB0GA1UdDgQWBBTAephojYn7qwVkDBF9qn1luMrMTjAfBgNVHSMEGDAWgBTAephojYn7qwVk\n"
                                    + "DBF9qn1luMrMTjANBgkqhkiG9w0BAQUFAAOCAQEANeMpauUvXVSOKVCUn5kaFOSPeCpilKInZ57Q\n"
                                    + "zxpeR+nBsqTP3UEaBU6bS+5Kb1VSsyShNwrrZHYqLizz/Tt1kL/6cdjHPTfStQWVYrmm3ok9Nns4\n"
                                    + "d0iXrKYgjy6myQzCsplFAMfOEVEiIuCl6rYVSAlk6l5PdPcFPseKUgzbFbS9bZvlxrFUaKnjaZC2\n"
                                    + "mqUPuLk/IH2uSrW4nOQdtqvmlKXBx4Ot2/Unhw4EbNX/3aBd7YdStysVAq45pmp06drE57xNNB6p\n"
                                    + "XE0zX5IJL4hmXXeXxx12E6nV5fEWCRE11azbJHFwLJhWC9kXtNHjUStedejV0NxPNO3CBWaAocvm\n"
                                    + "Mw==\n"
                                    + "-----END CERTIFICATE-----\n";
    //        Subject: C=US, O=Google Inc, CN=Google Internet Authority G2
    //        Issuer: C=US, O=GeoTrust Inc., CN=GeoTrust Global CA
    String cert3_googleG2_suspect = "-----BEGIN CERTIFICATE-----\n"
                                    + "MIID8DCCAtigAwIBAgIDAjp2MA0GCSqGSIb3DQEBBQUAMEIxCzAJBgNVBAYTAlVTMRYwFAYDVQQK\n"
                                    + "Ew1HZW9UcnVzdCBJbmMuMRswGQYDVQQDExJHZW9UcnVzdCBHbG9iYWwgQ0EwHhcNMTMwNDA1MTUx\n"
                                    + "NTU1WhcNMTYxMjMxMjM1OTU5WjBJMQswCQYDVQQGEwJVUzETMBEGA1UEChMKR29vZ2xlIEluYzEl\n"
                                    + "MCMGA1UEAxMcR29vZ2xlIEludGVybmV0IEF1dGhvcml0eSBHMjCCASIwDQYJKoZIhvcNAQEBBQAD\n"
                                    + "ggEPADCCAQoCggEBAJwqBHdc2FCROgajguDYUEi8iT/xGXAaiEZ+4I/F8YnOIe5a/mENtzJEiaB0\n"
                                    + "C1NPVaTOgmKV7utZX8bhBYASxF6UP7xbSDj0U/ck5vuR6RXEz/RTDfRK/J9U3n2+oGtvh8DQUB8o\n"
                                    + "MANA2ghzUWx//zo8pzcGjr1LEQTrfSTe5vn8MXH7lNVg8y5Kr0LSy+rEahqyzFPdFUuLH8gZYR/N\n"
                                    + "nag+YyuENWllhMgZxUYi+FOVvuOAShDGKuy6lyARxzmZEASg8GF6lSWMTlJ14rbtCMoU/M4iarNO\n"
                                    + "z0YDl5cDfsCx3nuvRTPPuj5xt970JSXCDTWJnZ37DhF5iR43xa+OcmkCAwEAAaOB5zCB5DAfBgNV\n"
                                    + "HSMEGDAWgBTAephojYn7qwVkDBF9qn1luMrMTjAdBgNVHQ4EFgQUSt0GFhu89mi1dvWBtrtiGrpa\n"
                                    + "gS8wEgYDVR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAQYwNQYDVR0fBC4wLDAqoCigJoYk\n"
                                    + "aHR0cDovL2cuc3ltY2IuY29tL2NybHMvZ3RnbG9iYWwuY3JsMC4GCCsGAQUFBwEBBCIwIDAeBggr\n"
                                    + "BgEFBQcwAYYSaHR0cDovL2cuc3ltY2QuY29tMBcGA1UdIAQQMA4wDAYKKwYBBAHWeQIFATANBgkq\n"
                                    + "hkiG9w0BAQUFAAOCAQEAJ4zP6cc7vsBv6JaE+5xcXZDkd9uLMmCbZdiFJrW6nx7eZE4fxsggWwmf\n"
                                    + "q6ngCTRFomUlNz1/Wm8gzPn68R2PEAwCOsTJAXaWvpv5Fdg50cUDR3a4iowx1mDV5I/b+jzG1Zgo\n"
                                    + "+ByPF5E0y8tSetH7OiDk4Yax2BgPvtaHZI3FCiVCUe+yOLjgHdDh/Ob0r0a678C/xbQF9ZR1DP6i\n"
                                    + "vgK66oZb+TWzZvXFjYWhGiN3GhkXVBNgnwvhtJwoKvmuAjRtJZOcgqgXe/GFsNMPWOH7sf6coaPo\n"
                                    + "/ck/9Ndx3L2MpBngISMjVROPpBYCCX65r+7bU2S9cS+5Oc4wt7S8VOBHBw=="
                                    + "-----END CERTIFICATE-----\n";

    @Test
    public void test() throws Exception {
        System.setProperty("javax.net.debug", "ssl");
        HttpClient httpclient = new ApacheHttpClientFactory().create(HttpClientSettings.adapt(new LegacyClientConfiguration()));
        //        HttpUriRequest request = new HttpGet("https://google.com:6443");
        HttpUriRequest request = new HttpGet("https://google.com");
        org.apache.http.HttpResponse resp = httpclient.execute(request);
        System.out.println(resp);
    }

    @Test
    public void testVerifyBogusCert() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate x509_bogus = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(cert1_googleBogus.getBytes(Charset.forName("UTF-8"))));
        X509Certificate x509_geoTrust_suspect = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(cert2_geoTrust_suspect.getBytes(Charset.forName("UTF-8"))));
        X509Certificate x509_googleG2_suspect = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(cert3_googleG2_suspect.getBytes(Charset.forName("UTF-8"))));
        x509_bogus.verify(x509_googleG2_suspect.getPublicKey());
        x509_googleG2_suspect.verify(x509_geoTrust_suspect.getPublicKey());
        x509_geoTrust_suspect.verify(x509_geoTrust_suspect.getPublicKey());
        // Note the geo trust suspect X509 has the same public key as the real geo trust X509
        X509Certificate x509_geo_trust = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(real_geo_trust.getBytes(Charset.forName("UTF-8"))));
        assertTrue(Arrays.equals(x509_geoTrust_suspect.getPublicKey().getEncoded(), x509_geo_trust.getPublicKey().getEncoded()));
        assertEquals(x509_geoTrust_suspect.getSubjectX500Principal(), x509_geo_trust.getSubjectX500Principal());
    }

    // Sanity check the real certs
    @Test
    public void testVerifyRealCert() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate x509_google = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(real_star_google_com.getBytes(Charset.forName("UTF-8"))));
        X509Certificate x509_geo_trust = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(real_google_g2.getBytes(Charset.forName("UTF-8"))));
        x509_google.verify(x509_geo_trust.getPublicKey());
    }

    @Test
    public void testVerify() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate x509_cert2_trusted = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(cert2_geoTrust_suspect.getBytes(Charset.forName("UTF-8"))));
        X509Certificate x509_googleG2_suspect = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(cert3_googleG2_suspect.getBytes(Charset.forName("UTF-8"))));
        X509Certificate x509_google_g2 = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(real_google_g2.getBytes(Charset.forName("UTF-8"))));
        X509Certificate x509_geo_trust = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(real_geo_trust.getBytes(Charset.forName("UTF-8"))));
        // The 2nd cert verifies.  Note it is self-signed, the other (real one) is not (self-signed).
        x509_cert2_trusted.getSubjectX500Principal().equals(x509_geo_trust.getSubjectX500Principal());
        x509_cert2_trusted.verify(x509_geo_trust.getPublicKey());
        // The 3rd cert verifies.
        x509_googleG2_suspect.verify(x509_geo_trust.getPublicKey());
        // The 3rd cert claims to be Google G2
        x509_googleG2_suspect.getSubjectX500Principal().equals(x509_google_g2.getSubjectX500Principal());
        assertEquals(x509_googleG2_suspect.getPublicKey(), x509_google_g2.getPublicKey());
        assertTrue(Arrays.equals(x509_googleG2_suspect.getPublicKey().getEncoded(), x509_google_g2.getPublicKey().getEncoded()));
    }
}
