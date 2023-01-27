package framework;

import com.github.davidmoten.rtree.geometry.Rectangle;

public interface RTreeObject {

	public Rectangle boundingbox();

	public RoadObject data();

}
