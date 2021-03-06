package ass2.spec;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;


/**
 * COMMENT: Comment HeightMap 
 *
 * @author malcolmr
 */
public class Terrain {
  
  private Dimension mySize;
  private double[][] myAltitude;
  private List<Tree> myTrees;
  private List<Enemy> myEnemies;
  private List<Road> myRoads;
  private float[] mySunlight;
  private List<PortalPair> myPortalPairs;
  
  /**
   * Create a new terrain
   *
   * @param width The number of vertices in the x-direction
   * @param depth The number of vertices in the z-direction
   */
  public Terrain(int width, int depth) {
    mySize = new Dimension(width, depth);
    myAltitude = new double[width][depth];
    myTrees = new ArrayList<Tree>();
    myEnemies = new ArrayList<Enemy>();
    myRoads = new ArrayList<Road>();
    mySunlight = new float[3];
    myPortalPairs = new ArrayList<PortalPair>();
  }
  
  public Terrain(Dimension size) {
    this(size.width, size.height);
  }
  
  public Dimension size() {
    return mySize;
  }
  
  public List<Tree> trees() {
    return myTrees;
  }
  
  public List<Enemy> enemies() {
    return myEnemies;
  }
  
  public List<PortalPair> portalpairs() {
    return myPortalPairs;
  }
  
  public List<Road> roads() {
    return myRoads;
  }
  
  public float[] getSunlight() {
    return mySunlight;
  }
  
  /**
   * Set the sunlight direction.
   *
   * Note: the sun should be treated as a directional light, without a position
   *
   * @param dx
   * @param dy
   * @param dz
   */
  public void setSunlightDir(float dx, float dy, float dz) {
    mySunlight[0] = dx;
    mySunlight[1] = dy;
    mySunlight[2] = dz;
  }
  
  /**
   * Resize the terrain, copying any old altitudes.
   *
   * @param width
   * @param height
   */
  public void setSize(int width, int height) {
    mySize = new Dimension(width, height);
    double[][] oldAlt = myAltitude;
    myAltitude = new double[width][height];
    
    for (int i = 0; i < width && i < oldAlt.length; i++) {
      for (int j = 0; j < height && j < oldAlt[i].length; j++) {
        myAltitude[i][j] = oldAlt[i][j];
      }
    }
  }
  
  /**
   * Get the altitude at a grid point
   *
   * @param x
   * @param z
   * @return
   */
  public double getGridAltitude(int x, int z) {
    return myAltitude[x][z];
  }
  
  /**
   * Set the altitude at a grid point
   *
   * @param x
   * @param z
   * @return
   */
  public void setGridAltitude(int x, int z, double h) {
    myAltitude[x][z] = h;
  }
  
  /**
   * Get the altitude at an arbitrary point.
   * Non-integer points should be interpolated from neighbouring grid points
   *
   * @param x point on x axis
   * @param z point on z axis
   * @return altitude at an arbitary point
   */
  public double altitude(double x, double z) {
    double altitude = 0;
  
    //Out of bounds, return default value
    if (x < 0 || x > mySize.width -1 || z < 0 || z > mySize.height -1 )
      return altitude;
  
    //Check trivial case
    if ((int)x == x && (int)z == z) {
      altitude = getGridAltitude((int)x, (int)z);
    } else {
      //Compute closest 'grid' int coordinates
      //We floor/ceil based on winding order (Right hand rule)
      //Thus we know the point is enclosed in grid made by 4 points (leftX, rightX, upperZ, lowerZ).
      double leftX = Math.floor(x);
      double rightX = Math.ceil(x);
      double upperZ = Math.floor(z);
      double lowerZ = Math.ceil(z);
      double hypotenuseX = (leftX + lowerZ) - z;
    
      if ((int)x == x) {  //X provided is int
        altitude = calcBilinearInterpolationZComponent(z, upperZ, lowerZ, x, x); //interpolate only Z component
      } else if ((int)z == z) { //Z provided is int
        altitude = calcBilinearInterpolationXComponent(x, leftX, rightX, z, z); //interpolate only X component
      } else if (x < hypotenuseX) { //Point exists in left triangle, interpolate using it
        altitude = calcBilinearInterpolation(x, leftX, leftX, rightX, z, lowerZ, upperZ, upperZ, hypotenuseX);
      } else { //x > hypotenuseX, point exists in right triangle, interpolate using it
        altitude = calcBilinearInterpolation(x, rightX, rightX, leftX, z, upperZ, lowerZ, lowerZ, hypotenuseX);
      }
    }
  
    return altitude;
  }
  
  /**
   * Helper bilinear interpolation function based on slides 39-41 week 5 lectures
   */
  private double calcBilinearInterpolationXComponent(double x, double x1, double x2, double z1, double z2) {
    return ((x - x1) / (x2 - x1)) * getGridAltitude((int)x2, (int)z2) +
      ((x2 - x) / (x2 - x1)) * getGridAltitude((int)x1, (int)z1);
  }
  
  /**
   * Helper bilinear interpolation function based on slides 39-41 week 5 lectures
   */
  private double calcBilinearInterpolationZComponent(double z, double z1, double z2, double x1, double x2) {
    return ((z - z1) / (z2 - z1)) * getGridAltitude((int)x2, (int)z2) +
      ((z2 - z) / (z2 - z1)) * getGridAltitude((int)x1, (int)z1);
  }
  
  /**
   * Helper bilinear interpolation function based on slides 39-41 week 5 lectures
   */
  private double calcBilinearInterpolation(double x, double x1, double x2, double x3,
                                           double z, double z1, double z2, double z3,
                                           double hypotenuseX) {
    return ((x - x1) / (hypotenuseX - x1)) * calcBilinearInterpolationZComponent(z, z1, z3, x1, x3) +
      ((hypotenuseX - x) / (hypotenuseX - x1)) * calcBilinearInterpolationZComponent(z, z1, z2, x1, x2);
  }
  
  /**
   * Add a tree at the specified (x,z) point.
   * The tree's y coordinate is calculated from the altitude of the terrain at that point.
   *
   * @param x
   * @param z
   */
  public void addTree(double x, double z) {
    double y = altitude(x, z);
    Tree tree = new Tree(x, y, z);
    myTrees.add(tree);
  }
  
  
  /**
   * Add a road.
   *
   * @param x
   * @param z
   */
  public void addRoad(double width, double[] spine) {
    Road road = new Road(width, spine, this);
    myRoads.add(road);
  }
  
  /*********************** My Code *********************/
  
  public void draw(GL2 gl, TexturePack texturePack, int shaderProgram, Game.FRAGMENT_SHADER_MODE fragmentShaderColourMode,
                   boolean curLighting, boolean nightMode, float[] torchPosition) {
    gl.glPushMatrix();
    gl.glPushAttrib(GL2.GL_LIGHTING);
    
    gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
    
    //Get terrain texture
    Texture terrain = texturePack.getTerrain();
    terrain.enable(gl);
    terrain.bind(gl);
    TextureCoords textureCoords = terrain.getImageTexCoords();
  
    //Set terrain material
    float[] ambient = {0.2f, 0.25f, 0.2f, 1.0f};
    float[] diffuse = {0.2f, 0.6f, 0.3f, 1.0f};
    float[] specular = {0.0f, 0.0f, 0.0f, 1.0f};
  
    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambient, 0);
    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuse, 0);
    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specular, 0);
    
    //Draw the terrain one grid point at a time
    //Each grid contains 2 triangles
    for (int z = 0; z < mySize.height -1; ++z) {
      for (int x = 0; x < mySize.width -1; ++x) {
        //Fir
        double[] v1 = {x, getGridAltitude(x, z), z};
        double[] v2 = {x, getGridAltitude(x, z + 1), z + 1};
        double[] v3 = {x + 1, getGridAltitude(x + 1, z), z};
  
        double[] v1Texture = {textureCoords.left(), textureCoords.bottom()};
        double[] v2Texture = {textureCoords.right(), textureCoords.bottom()};
        double[] v3Texture = {textureCoords.left(), textureCoords.top()};
        
        double[] normal1 = MathUtil.getNormal(v1, v2, v3);
        gl.glNormal3dv(normal1, 0);
        
        //Draw first triangle in grid
        gl.glBegin(GL2.GL_TRIANGLES);
        {
          gl.glColor3f(0.0f, 1.0f, 0.0f); //Green colour (does nothing if lighting enabled)
          gl.glTexCoord2dv(v1Texture, 0);
          gl.glVertex3dv(v1, 0);
          gl.glTexCoord2dv(v2Texture, 0);
          gl.glVertex3dv(v2, 0);
          gl.glTexCoord2dv(v3Texture, 0);
          gl.glVertex3dv(v3, 0);
        }
        gl.glEnd();
  
        double[] v4 = {x + 1, getGridAltitude(x + 1, z), z};
        double[] v5 = {x, getGridAltitude(x, z + 1), z + 1};
        double[] v6 = {x + 1, getGridAltitude(x + 1, z + 1), z + 1};
  
        double[] v4Texture = {textureCoords.left(), textureCoords.top()};
        double[] v5Texture = {textureCoords.right(), textureCoords.bottom()};
        double[] v6Texture = {textureCoords.right(), textureCoords.top()};
  
        double[] normal2 = MathUtil.getNormal(v4, v5, v6);
        gl.glNormal3dv(normal2, 0);
  
        //Draw second triangle in grid
        gl.glBegin(GL2.GL_TRIANGLES);
        {
          gl.glColor3f(0.0f, 1.0f, 0.0f); //Green colour (does nothing if lighting enabled)
          gl.glTexCoord2dv(v4Texture, 0);
          gl.glVertex3dv(v4, 0);
          gl.glTexCoord2dv(v5Texture, 0);
          gl.glVertex3dv(v5, 0);
          gl.glTexCoord2dv(v6Texture, 0);
          gl.glVertex3dv(v6, 0);
        }
        gl.glEnd();
      }
    }
    gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
    terrain.disable(gl); //turn off terrain
  
    //Draw all trees part of terrain
    for (Tree tree : myTrees) {
      tree.draw(gl, texturePack);
    }
    
    //Draw all roads part of terrain
    for (Road road : myRoads) {
      road.draw(gl, texturePack);
    }
    
    //Draw all enemies on terrain
    for (Enemy enemy : myEnemies) {
      enemy.draw(gl, texturePack, shaderProgram, fragmentShaderColourMode, curLighting, nightMode, torchPosition);
    }
    
    //Draw all portal pairs
    for (PortalPair pp : myPortalPairs) {
      pp.draw(gl, texturePack);
    }
    
    gl.glPopAttrib();
    gl.glPopMatrix();
  }
  
  /**
   * Add a enemy at the specified (x,z) point with some rotation.
   *
   * @param x x axis coordinate of enemy
   * @param z z axis coordinate of enemy
   * @param rotation rotation (along y axis) that enemy should be facing
   */
  public void addEnemy(double x, double z, double rotation) {
    Enemy enemy = new Enemy(this, x, z, rotation);
    myEnemies.add(enemy);
  }
  
  /**
   * Add a portal pair to the terrain.
   *
   * @param firstX x axis of first portal
   * @param firstZ z axis of first portal
   * @param firstRotation rotation (along y axis) that first portal should be facing
   * @param secondX x axis of second portal
   * @param secondZ z axis of second portal
   * @param secondRotation rotation (along y axis) that second portal should be facing
   */
  public void addPortalPair(double firstX, double firstZ, double firstRotation,
                            double secondX, double secondZ, double secondRotation) {
    PortalPair pp = new PortalPair(this, firstX, firstZ, firstRotation, secondX, secondZ, secondRotation);
    myPortalPairs.add(pp);
  }
  
}
