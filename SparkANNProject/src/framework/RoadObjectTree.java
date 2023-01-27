package framework;

import com.github.davidmoten.rtree.geometry.Rectangle;

public class RoadObjectTree implements RTreeObject {
	private RoadObject roadObject;

	public RoadObjectTree(RoadObject roadObject) {
		this.roadObject = roadObject;
	}

	@Override
	public Rectangle boundingbox() {
		// TODO Auto-generated method stub

		double longitude = roadObject.getLongitude();
		double latitude = roadObject.getLatitude();
		return com.github.davidmoten.rtree.geometry.Geometries.rectangle(longitude, latitude, longitude, latitude);
	}

	@Override
	public RoadObject data() {
		// TODO Auto-generated method stub
		return roadObject;
	}

}
