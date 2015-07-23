package net.ripe.rpki.validator.models.validation

import java.net.URI

import net.ripe.rpki.commons.validation.ValidationStatus
import net.ripe.rpki.validator.models.{InvalidObject, RepoService, ValidatedObject}
import net.ripe.rpki.validator.store.CacheStore
import net.ripe.rpki.validator.support.ValidatorTestCase
import net.ripe.rpki.validator.util.TrustAnchorLocator
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar

import scalaz.Validation

class TrustAnchorValidationProcessTest extends ValidatorTestCase with MockitoSugar with BeforeAndAfter {
  
  val mockStore = mock[CacheStore]
  val mockRepoService = mock[RepoService]
  val mockTrustAnchorLocator: TrustAnchorLocator = mock[TrustAnchorLocator]

  val maxStaleDays: Int = 1

  val taName: String = "ripe"

  val enableLooseValidation: Boolean = true

  val taCertUri = new URI("rsync://taCert.cert")

  val matchingCert = mock[CertificateObject]

  val taValidatorProcess = new TrustAnchorValidationProcess(
    mockTrustAnchorLocator,
    mockStore,
    mockRepoService,
    maxStaleDays,
    taName,
    enableLooseValidation) {

    override def keyInfoMatches(certificate: CertificateObject): Boolean = certificate == matchingCert
  }

  before {
    when(mockTrustAnchorLocator.getCertificateLocation).thenReturn(taCertUri)
    when(mockRepoService.visitTrustAnchorCertificate(taCertUri)).thenReturn(Seq())
  }
  
  test("Should return validObject for trust anchor certificate without errors") {
    when(mockStore.getCertificates(taCertUri.toString)).thenReturn(Seq(matchingCert))

    val validation: Validation[String, Map[URI, ValidatedObject]] = taValidatorProcess.runProcess()

    val validatedObject: ValidatedObject = validation.toOption.get.get(taCertUri).get
    validatedObject.isInstanceOf[InvalidObject] should be(false)
    validatedObject.validationStatus should equal(ValidationStatus.PASSED)
  }

  test("Should return inValidObject when no valid ta certificate found") {
    when(mockStore.getCertificates(taCertUri.toString)).thenReturn(Seq())

    val validation: Validation[String, Map[URI, ValidatedObject]] = taValidatorProcess.runProcess()

    val validatedObject: ValidatedObject = validation.toOption.get.get(taCertUri).get
    validatedObject.isInstanceOf[InvalidObject] should be(true)
    validatedObject.validationStatus should equal(ValidationStatus.ERROR)
  }

  test("Should return inValidObject when more than one matching ta certificate found") {
    when(mockStore.getCertificates(taCertUri.toString)).thenReturn(Seq(matchingCert, matchingCert))

    val validation: Validation[String, Map[URI, ValidatedObject]] = taValidatorProcess.runProcess()

    val validatedObject: ValidatedObject = validation.toOption.get.get(taCertUri).get
    validatedObject.isInstanceOf[InvalidObject] should be(true)
    validatedObject.validationStatus should equal(ValidationStatus.ERROR)
  }

  test("Should just warn when more than one object is found with the uri of the ta certificate but only one matches the ta certificate") {
    val cert2 = mock[CertificateObject]
    when(mockStore.getCertificates(taCertUri.toString)).thenReturn(Seq(matchingCert, cert2))

    val validation: Validation[String, Map[URI, ValidatedObject]] = taValidatorProcess.runProcess()

    val validatedObject: ValidatedObject = validation.toOption.get.get(taCertUri).get
    validatedObject.isInstanceOf[InvalidObject] should be(false)
    validatedObject.validationStatus should equal(ValidationStatus.WARNING)
  }
}