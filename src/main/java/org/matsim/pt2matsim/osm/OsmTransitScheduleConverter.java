/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package org.matsim.pt2matsim.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt2matsim.osm.lib.Osm;
import org.matsim.pt2matsim.osm.lib.OsmData;
import org.matsim.pt2matsim.osm.lib.OsmDataImpl;
import org.matsim.pt2matsim.osm.lib.OsmFileReader;
import org.matsim.pt2matsim.osm.parser.TagFilter;

import java.util.*;

/**
 * Creates an unmapped MATSim transit schedule from OSM. Converts
 * available public transit data from OSM to a schedule: stop facilities,
 * transitRoutes and routeProfiles. Departures and link sequences are missing.
 *
 * todo implement new framwork
 * @author polettif
 */
public class OsmTransitScheduleConverter {

	private static final Logger log = Logger.getLogger(OsmTransitScheduleConverter.class);

	private final CoordinateTransformation transformation;
	private final TransitSchedule transitSchedule;
	private final TransitScheduleFactory factory;
	private final String osmInput;
//	private OsmXmlParserHandler handler;

	// filters
	private final TagFilter stop_area;
	private final TagFilter stop_position;
	private final TagFilter route_master;
	private final TagFilter ptRoute;
	private final OsmData osmData;

	private int routeNr = 0;

	public OsmTransitScheduleConverter(TransitSchedule schedule, CoordinateTransformation transformation, String osmInput) {
		this.transitSchedule = schedule;
		this.transformation = transformation;
		this.osmInput = osmInput;

		this.osmData = new OsmDataImpl();
		new OsmFileReader(osmData).readFile(osmInput);


		this.factory = transitSchedule.getFactory();

		// initialize filters
		stop_position = new TagFilter(Osm.ElementType.NODE);
		stop_position.add(Osm.Key.PUBLIC_TRANSPORT, Osm.Value.STOP_POSITION);

		stop_area = new TagFilter(Osm.ElementType.RELATION);
		stop_area.add(Osm.Key.PUBLIC_TRANSPORT, Osm.Value.STOP_AREA);

		route_master = new TagFilter(Osm.ElementType.RELATION);
		route_master.add(Osm.Key.ROUTE_MASTER, Osm.Value.BUS);
		route_master.add(Osm.Key.ROUTE_MASTER, Osm.Value.TROLLEYBUS);
		route_master.add(Osm.Key.ROUTE_MASTER, Osm.Value.TRAM);
		route_master.add(Osm.Key.ROUTE_MASTER, Osm.Value.MONORAIL);
		route_master.add(Osm.Key.ROUTE_MASTER, Osm.Value.SUBWAY);
		route_master.add(Osm.Key.ROUTE_MASTER, Osm.Value.FERRY);

		ptRoute = new TagFilter(Osm.ElementType.RELATION);
		ptRoute.add(Osm.Key.ROUTE, Osm.Value.BUS);
		ptRoute.add(Osm.Key.ROUTE, Osm.Value.TROLLEYBUS);
		ptRoute.add(Osm.Key.ROUTE, Osm.Value.RAIL);
		ptRoute.add(Osm.Key.ROUTE, Osm.Value.TRAM);
		ptRoute.add(Osm.Key.ROUTE, Osm.Value.LIGHT_RAIL);
		ptRoute.add(Osm.Key.ROUTE, Osm.Value.FUNICULAR);
		ptRoute.add(Osm.Key.ROUTE, Osm.Value.MONORAIL);
		ptRoute.add(Osm.Key.ROUTE, Osm.Value.SUBWAY);
	}

	/**
	 * Parses the osm file and converts it to a schedule
	 */
	public void run() {
		parse();
		convert();
	}

	private void parse() {
		/*
		TagFilter nodeFilter = new TagFilter(Osm.ElementType.NODE);
		nodeFilter.add(Osm.Key.PUBLIC_TRANSPORT, Osm.Value.STOP_POSITION);

		TagFilter wayFilter = new TagFilter(Osm.ElementType.WAY);

		TagFilter relationFilter = new TagFilter(Osm.ElementType.RELATION);
		relationFilter.add(Osm.Key.ROUTE, Osm.Value.BUS);
		relationFilter.add(Osm.Key.ROUTE, Osm.Value.TROLLEYBUS);
		relationFilter.add(Osm.Key.ROUTE, Osm.Value.RAIL);
		relationFilter.add(Osm.Key.ROUTE, Osm.Value.TRAM);
		relationFilter.add(Osm.Key.ROUTE, Osm.Value.LIGHT_RAIL);
		relationFilter.add(Osm.Key.ROUTE, Osm.Value.FUNICULAR);
		relationFilter.add(Osm.Key.ROUTE, Osm.Value.MONORAIL);
		relationFilter.add(Osm.Key.ROUTE, Osm.Value.SUBWAY);
		relationFilter.add(Osm.Key.ROUTE_MASTER, Osm.Value.BUS);
		relationFilter.add(Osm.Key.ROUTE_MASTER, Osm.Value.TROLLEYBUS);
		relationFilter.add(Osm.Key.ROUTE_MASTER, Osm.Value.TRAM);
		relationFilter.add(Osm.Key.ROUTE_MASTER, Osm.Value.MONORAIL);
		relationFilter.add(Osm.Key.ROUTE_MASTER, Osm.Value.SUBWAY);
		relationFilter.add(Osm.Key.ROUTE_MASTER, Osm.Value.FERRY);
		*/

//		handler = new OsmXmlParserHandler(nodeFilter, wayFilter, relationFilter);

//		OsmParser parser = new OsmParser();
//		parser.addHandler(handler);
//		parser.run(osmInput);
	}

	/**
	 * Converts relations, nodes and ways from osm to an
	 * unmapped MATSim Transit Schedule
	 */
	private void convert() {
		Map<Id<TransitLine>, TransitLine> transitLinesDump = new HashMap<>();

		Map<Id<Osm.Node>, Osm.Node> nodes = osmData.getNodes();
		Map<Id<Osm.Relation>, Osm.Relation> relations = osmData.getRelations();
		Map<Id<Osm.Way>, Osm.Way> ways = osmData.getWays();

		/**
		 * Create TransitStopFacilities from public_transport=stop_position
		 */
		createStopFacilities();

		/**
		 * https://wiki.openstreetmap.org/wiki/Relation:route_master
		 */
		Set<Osm.Relation> routesWithMaster = new HashSet<>();


		/**
		 * Create transitLines via route_masters
		 */
		for(Osm.Relation relation : relations.values()) {
			if(route_master.matches(relation.getTags())) {
				Id<TransitLine> lineId = createLineId(relation);
				TransitLine newTransitLine = factory.createTransitLine(lineId);
				newTransitLine.setName(relation.getTags().get(Osm.Key.NAME));

				for(Osm.Element member : relation.getMembers()) {
					Osm.Relation routeRel = (Osm.Relation) member;
					// maybe member route does not exist in area
					if(routeRel != null) {
						TransitRoute newTransitRoute = createTransitRoute(routeRel);
						if(newTransitRoute != null) {
							newTransitLine.addRoute(newTransitRoute);
							routesWithMaster.add(routeRel);
						}
					}
				}
				transitLinesDump.put(lineId, newTransitLine);
			}
		}

		/**
		 * Create transitRoutes without route_masters
		 */
		for(Osm.Relation relation : relations.values()) {
			if(ptRoute.matches(relation.getTags()) && !routesWithMaster.contains(relation)) {
				Id<TransitLine> lineId = createLineId(relation);

				if(!transitLinesDump.containsKey(lineId)) {
					transitLinesDump.put(lineId, factory.createTransitLine(lineId));
				}

				TransitLine transitLine = transitLinesDump.get(lineId);

				TransitRoute newTransitRoute = createTransitRoute(relation);
				if(newTransitRoute != null) {
					transitLine.addRoute(newTransitRoute);
				}
			}
		}

		// add lines to schedule
		for(TransitLine transitLine : transitLinesDump.values()) {
				this.transitSchedule.addTransitLine(transitLine);
		}

		log.info("MATSim Transit Schedule created.");
	}

	/**
	 * creates stop facilities from nodes and adds them to the schedule
	 */
	private void createStopFacilities() {
		Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities = this.transitSchedule.getFacilities();

		// create facilities from stop_area first
		for(Osm.Relation relation : osmData.getRelations().values()) {
			if(stop_area.matches(relation.getTags())) {
				String stopPostAreaId = relation.getValue(Osm.Key.NAME);

				// create a facility for each member
				for(Osm.Element member : relation.getMembers()) {
					if(relation.getMemberRole(member).equals(Osm.Value.STOP)) {
						Osm.Node n = (Osm.Node) member;
						TransitStopFacility newStopFacility = createStopFacilityFromOsmNode(n, stopPostAreaId);

						if(!stopFacilities.containsValue(newStopFacility)) {
							this.transitSchedule.addStopFacility(newStopFacility);
						}
					}
				}
			}
		}

		// create other facilities
		for(Osm.Node node : osmData.getNodes().values()) {
			if(stop_position.matches(node.getTags())) {
				if(!stopFacilities.containsKey(Id.create(node.getId(), TransitStopFacility.class))) {
					this.transitSchedule.addStopFacility(createStopFacilityFromOsmNode(node));
				}
			}
		}
	}

	/**
	 * creates a TransitStopFacility from an Node
	 * @return the created facility
	 */
	private TransitStopFacility createStopFacilityFromOsmNode(Osm.Node node, String stopPostAreaId) {
		Id<TransitStopFacility> id = Id.create(node.getId(), TransitStopFacility.class);
		Coord coord = transformation.transform(node.getCoord());
		TransitStopFacility newStopFacility = factory.createTransitStopFacility(id, coord, false);
		newStopFacility.setName(node.getValue(Osm.Key.NAME));
		if(stopPostAreaId != null ) { newStopFacility.setStopPostAreaId(stopPostAreaId); }
		return newStopFacility;
	}

	private TransitStopFacility createStopFacilityFromOsmNode(Osm.Node node) {
		return createStopFacilityFromOsmNode(node, null);
	}

	/**
	 * Creates a TransitRoute from a relation.
	 * @return <code>null</code> if the route has stops outside of the area
	 */
	private TransitRoute createTransitRoute(Osm.Relation relation) {
		List<TransitRouteStop> stopSequenceForward = new ArrayList<>();

		// create different RouteStops and stopFacilities for forward and backward
		for(int i = 0; i < relation.getMembers().size() - 1; i++) {
			Osm.Element member = relation.getMembers().get(i);

			// route Stops
			if(member.getType().equals(Osm.ElementType.NODE) && (Osm.Value.STOP.equals(relation.getMemberRole(member)) || Osm.Value.STOP_FORWARD.equals(relation.getMemberRole(member)))) {
				Id<TransitStopFacility> id = Id.create(member.getOsmId(), TransitStopFacility.class);
				TransitStopFacility transitStopFacility = transitSchedule.getFacilities().get(id);
				if(transitStopFacility == null) {
					return null;
				}
				// create transitRouteStop
				TransitRouteStop newRouteStop = factory.createTransitRouteStop(transitStopFacility, 0.0, 0.0);
				stopSequenceForward.add(newRouteStop);
			}

			// route links
//			if(member.type.equals(OsmParser.OsmRelationMemberType.WAY) && !OsmValue.BACKWARD.equals(member.role)) {
//				linkSequenceForward.add(Id.createLinkId(member.refId));
//			}
		}

//		NetworkRoute networkRoute = (linkSequenceForward.size() == 0 ? null : RouteUtils.createNetworkRoute(linkSequenceForward, null));
		if(stopSequenceForward.size() == 0){
			return null;
		}

		// one relation has two routes, forward and back
		Id<TransitRoute> transitRouteId = Id.create(createStringId(relation)+ (++routeNr), TransitRoute.class);
		TransitRoute newTransitRoute = factory.createTransitRoute(transitRouteId, null, stopSequenceForward, relation.getTags().get(Osm.Key.ROUTE));
		newTransitRoute.addDeparture(factory.createDeparture(Id.create("departure" + routeNr, Departure.class), 60.0));

		return newTransitRoute;
	}

	private Id<TransitLine> createLineId(Osm.Relation relation) {
		return Id.create(createStringId(relation), TransitLine.class);
	}

	private String createStringId(Osm.Relation relation) {
		String id;
		boolean ref = false, operator=false, name=false;

		if(relation.getTags().containsKey("name")) { name = true; }
		if(relation.getTags().containsKey("ref")) { ref = true; }
		if(relation.getTags().containsKey("operator")) { operator = true; }

		if(operator && ref) {
			id = relation.getValue("operator")+": "+relation.getValue("ref");
		}
		else if(operator && name) {
			id = relation.getValue("operator")+": "+relation.getValue("name");
		}
		else if(ref){
			id = relation.getValue("ref");
		}
		else if(name) {
			id = relation.getValue("name");
		}
		else {
			id = relation.getId().toString();
		}

		return id;
	}

	private Id<TransitLine> createLineId2(Osm.Relation relation) {
		String id;
		boolean ref = false, operator=false, name=false;


		if(relation.getTags().containsKey("ref")) { ref = true; }
		if(relation.getTags().containsKey("operator")) { operator = true; }
		if(relation.getTags().containsKey("name")) { name = true; }

		if(operator && ref) {
			id = relation.getValue("operator")+"_"+relation.getValue("ref");
		}
		else if(operator && name) {
			id = relation.getValue("operator")+"_"+relation.getValue("ref");
		}
		else if(name) {
			id = relation.getValue("name");
		}
		else if(ref){
			id = relation.getValue("ref");
		}
		else {
			id = relation.getId().toString();
		}

		try {
			return Id.create(id, TransitLine.class);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}