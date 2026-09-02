package com.kevel.apso.aws

import org.specs2.mutable.Specification
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.model.S3Exception

class S3BucketSpec extends Specification {

  // The message the CRT-based S3 client builds for a failed request, as seen in production.
  private def clientError(errorString: String) =
    SdkClientException.create(s"Failed to send the request: $errorString")

  private def serviceError(statusCode: Int, errorCode: String) =
    S3Exception
      .builder()
      .statusCode(statusCode)
      .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).serviceName("S3").build())
      .message("Boom")
      .build()

  "An S3Bucket" should {

    "recognize a slow down" in {

      "reported as a service error" in {
        S3Bucket.isSlowDown(serviceError(503, "SlowDown")) must beTrue
      }

      "reported as a service error with a 429 status code" in {
        S3Bucket.isSlowDown(serviceError(429, "TooManyRequestsException")) must beTrue
      }

      "reported by the CRT client as a client-side error" in {
        S3Bucket.isSlowDown(clientError(S3Bucket.SlowDownErrorMessage)) must beTrue
      }
    }

    "not recognize as a slow down" in {

      // A 503 alone doesn't imply throttling, so it must keep being handled as a plain service error.
      "a service error with a 503 status code but no throttling error code" in {
        S3Bucket.isSlowDown(serviceError(503, "ServiceUnavailable")) must beFalse
      }

      "a service error reporting another failure" in {
        S3Bucket.isSlowDown(serviceError(404, "NoSuchKey")) must beFalse
      }

      "a client-side error reporting another failure" in {
        S3Bucket.isSlowDown(clientError("Socket closed")) must beFalse
      }

      "a client-side error without a message" in {
        S3Bucket.isSlowDown(SdkClientException.builder().build()) must beFalse
      }
    }
  }
}
