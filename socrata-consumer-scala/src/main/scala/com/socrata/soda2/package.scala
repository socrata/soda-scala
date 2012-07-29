package com.socrata

import scala.{collection => sc}

package object soda2 {
  /** A map with case-insensitive keys containing all the SODA2-specific
   * metadata that is returned with a response.  In the HTTP case, this
   * means all the "X-SODA2-" HTTP headers. */
  type Soda2Metadata = sc.Map[String, String] // FIXME: this type alias is not correct; it must guarantee case-insensitivity
}
