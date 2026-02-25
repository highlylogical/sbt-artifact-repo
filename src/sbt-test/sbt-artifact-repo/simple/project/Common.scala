import sbt.Credentials
import sbt.librarymanagement.ivy.DirectCredentials
import sbt.Resolver
import sbt.MavenRepository
import org.scalatest.matchers.should._
import org.scalatest.matchers.should.Matchers

object TestUtils extends Matchers {

    case class Expectations(publish: Option[MavenRepository], resolvers: Seq[MavenRepository], credentials: Seq[DirectCredentials])

    import org.scalactic.Equality

    implicit val directCredentialsEquality: Equality[DirectCredentials] = new Equality[DirectCredentials] {
        override def areEqual(a: DirectCredentials, b: Any): Boolean = b match {
            case other: DirectCredentials =>
            a.realm == other.realm &&
            a.host.equalsIgnoreCase(other.host) && // Case-insensitive comparison for host
            a.userName == other.userName &&        // Exact match for username
            a.passwd == other.passwd               // Exact match for password
            case _ => false
        }
    }

    def compareCredentials(expected: Seq[DirectCredentials], actual: Seq[Credentials]) = {
        expected.forall { dc =>
            actual.exists {
                case adc: DirectCredentials =>
                    adc.realm == dc.realm &&
                    adc.host.toLowerCase() == dc.host.toLowerCase() &&
                    adc.userName == dc.userName &&
                    adc.passwd == dc.passwd
                case _ => false
            }
        }
    }

    def publishToShouldBe(expected: Option[MavenRepository], actual: Option[Resolver]) =

        expected.forall { er =>
            actual.exists {
                case ar: MavenRepository =>
                    er.root == ar.root && er.name == ar.name
                case _ => false
            }
        }

    def compareResolvers(expected: Seq[MavenRepository], actual: Seq[Resolver]): Boolean =
        expected.forall { er =>
            actual.exists {
                case ar: MavenRepository => er.root == ar.root && er.name == ar.name
                case _ => false
            }
        }

    // def compareCredentials(expected: DirectCredentials, actual: DirectCredentials) = {
    //     actual should have {
    //         'realm (expected.realm)
    //         'host (expected.host)
    //         'userName (expected.userName)
    //         'passwd (expected.passwd)
    //     }
    // }
}