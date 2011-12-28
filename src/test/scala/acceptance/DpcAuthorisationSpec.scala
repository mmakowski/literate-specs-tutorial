package acceptance

import org.specs2._
import org.specs2.specification.{Given, When}

import org.junit.runner._
import runner._

@RunWith(classOf[JUnitRunner])
class DpcAuthorisationSpec extends Specification { def is = literate ^ 
"Authorisation with DPC".title ^
"""
When our users are served documents that originate from Data Provider Corporation (DPC), our system is required to check with DPC that the user has been authorised to read the document; this also allows DPC to track the usage and bill us accordingly. 

DPC provides a web service that performs the authorisation. Our system works out whether the document accessed was provided by DPC and if so, invokes the plug-in to authorise against the DPC web service.

Let's say that within DPC user `${g.osborne}` has the following permissions:

Document Id          | Access Allowed
---------------------|----------------
`${budgets/uk/2011}` | ${yes}
`${budgets/us/2012}` | ${no}

""" ^ dpcPermissions ^ """
### User is given access if allowed by DPC

When the user tries to access document `${budgets/uk/2011}`, which is provided by DPC, the system makes a call to the plug-in providing the user id and document id.
""" ^ documentId ^ """
Based on the information provided, the plug-in works out the request parameters for the DPC web service:

----------------|--------------------
`${systemId}`   | `${42}`
`${userId}`     | `${g.osborne}`
`${documentId}` | `${budgets/uk/2011}`

and makes a call to the service, which responds:

    ${ALLOW}
""" ^ requestAndResponse ^ """
as a result the user is given access to the document.

### User is denied access if denied by DPC

When the same user tries to access another document provided by DPC, `budgets/us/2012`, the system again calls the plug-in, which makes a call to the web service with parameters:

-------------|--------------------
`systemId`   | `42`
`userId`     | `g.osborne`
`documentId` | `budgets/us/2012`

This time the service responds:

    DENY

as a result of which the user is denied the access to this document.
""" ^ end

  // 1. DPC permissions 

  private class DpcPermissions(val userId: String, val permissionMap: Map[String, Boolean])
  
  private object dpcPermissions extends Given[DpcPermissions] {
    def extract(text: String) = {
      val userId::permissions = extractAll(text)
      val permissionMap = mkMap (permissions) mapValues (_ == "yes")
      new DpcPermissions(userId, permissionMap)
    }
  }

  // 2. Document id 

  private class DocumentId(val documentId: String, previousContext: DpcPermissions) 
      extends DpcPermissions(previousContext.userId, previousContext.permissionMap)
  
  private object documentId extends When[DpcPermissions, DocumentId] {
    def extract(previousContext: DpcPermissions, text: String) = {
      val documentId = extract1(text)
      new DocumentId(documentId, previousContext)
    }
  }

  // 3. Request and response 

  private class RequestAndResponse(val expectedRequestParams: Map[String, String], val response: String, previousContext: DocumentId)
      extends DocumentId(previousContext.documentId, previousContext)
  
  private object requestAndResponse extends When[DocumentId, RequestAndResponse] {
    def extract(previousContext: DocumentId, text: String) = {
      val tokens = extractAll(text)
      val expectedRequestParams = mkMap (tokens.init)
      val response = tokens.last
      new RequestAndResponse(expectedRequestParams, response, previousContext)
    }  
  }
  

  // utilities

  private def mkMap(s: Seq[String]) = (s grouped 2 map mkPair) toMap
  private def mkPair(s: Seq[String]) = (s(0), s(1))
}