/**
 * The BSD License
 *
 * Copyright (c) 2010, 2011 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.rpki.validator
package views

import models.Roas
import models.RtrPrefix
import scala.collection.JavaConverters._
import net.ripe.ipresource.Asn
import net.ripe.ipresource.IpRange
import lib.NumberResources._

abstract class RoaTableData(roas: Roas) extends DataTableJsonView[RoaTableRecord] {

  override def getAllRecords() = {
    val (ready, loading) = roas.all.partition(_._2.isDefined)
    val records = for {
      (talName, Some(roas)) <- ready
      validatedRoa <- roas
      roa = validatedRoa.roa
      roaPrefix <- roa.getPrefixes().asScala
    } yield {
      new RoaTableRecord(
        asn = roa.getAsn(),
        prefix = roaPrefix.getPrefix(),
        maxPrefixLength = roaPrefix.getEffectiveMaximumLength(),
        trustAnchorName = talName)
    }
    records.toIndexedSeq
  }

  override def filter(searchCriterium: Any): RoaTableRecord => Boolean = {
    searchCriterium match {
      case iprange: IpRange => _.prefix.overlaps(iprange)
      case asn: Asn => _.asn == asn
      case searchString: String =>
        (record =>
          searchString.isEmpty() ||
            record.asn.toString().contains(searchString) ||
            record.prefix.toString().contains(searchString) ||
            record.maxPrefixLength.toString().contains(searchString) ||
            record.trustAnchorName.toUpperCase().contains(searchString))
      case _ => _ => true
    }
  }

  override def ordering(sortColumn: Int) = {
    sortColumn match {
      case 0 => AsnOrdering.on(_.asn)
      case 1 => IpRangeOrdering.on(_.prefix)
      case 2 => implicitly[Ordering[Int]].on(_.maxPrefixLength)
      case 3 => implicitly[Ordering[String]].on(_.trustAnchorName)
      case _ => sys.error("unknown sort column: " + sortColumn)
    }
  }

  override def getValuesForRecord(record: RoaTableRecord) = {
    List(record.asn.getValue().toString(), record.prefix.toString(), record.maxPrefixLength.toString(), record.trustAnchorName)
  }
}

case class RoaTableRecord(asn: Asn, prefix: IpRange, maxPrefixLength: Int, trustAnchorName: String)
