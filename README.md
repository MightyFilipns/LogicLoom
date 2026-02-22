# MCChipMaker
A Minecraft mod for creating logic circuit and computers

# Tutorial
## Logic synthesis
Make sure you have installed Yosys.
First write the desired logic in Verilog 2001.
Then take the Synth.ys script from this repo and add read_verilog commands to the top of the file to read your design.

Run the script with
>yosys Synth.ys

The output JSON will be called `mc_chip_maker.json` load this file into MC using
>`/mcchipmaker load_json FILE_PATH

### Limitations 
Ports with Inout as direction are unsupported
The JSON file must only have one module. Only the first module is read all others are ignored.

## Setup parameters

Maximum Iterations for the placement step
>`/mcchipmaker param max_iter NUMBER_HERE

Size of the placement area along X and Y axis
>`/mcchipmaker param chip_size NUMBER_HERE

Force initial force multiplier used during spreading cells during placement
Should be a small value. Default is 0.05
>`/mcchipmaker param force_const NUMBER_HERE

Number by which to multiply the force multiplier every placement iteration
Should be a small value. Default is 1.047
>`/mcchipmaker param force_mul NUMBER_HERE

When a straight wire between two point can't be run because of obstacles. A wave propagation algorithm is used.
In order to sped up this algorithm a bounding box is constructed using the two points. The wave can go outside this box by a maximum of `router_max_search`.
Default is 13
>`/mcchipmaker param router_max_search NUMBER_HERE 

Path to the file made by Yosys
>`/mcchipmaker load_json FILE_PATH

### Fixed points

Fixed points are like other cells, but they have a fixed position. They are used to move the design as needed.
You will need to add at least one fixed point in order to get useful results.
Currently, you can only add fixed points that are attached to every cell in the design.

All ports in a design are a fixed point attached to the cells they are connected to with weights of 1.

Position of fixed point is stored relative to the current starting point.
If the starting point is changed after a fixed point is added the position of that fixed point in the world will change.

To modify fixed points use
>/chipmaker fixed_points


## P&R
Place the logic gates
Make sure that the area is completely flat and on all ground blocks redstone wire and repeaters can be placed
>`/mcchipmaker place
 
Place the Wires
>`/mcchipmaker route


## Fixing bad wires
Currently, the wire placing algorithm has bugs so run this command can be used to check for broken wires
This command compares starting point of all wires and checks if the power is the same at all ends.
If not problematic ones are printed in the chat. If there are any issues they are usually obvious.

This command can find bad wires even if everything appears to behave correctly as those bad wires might not be currently affecting the output.
You must wait for all signals to propagate otherwise you will bad results.
You can speed this up with /tick sprint
>`/mcchipmaker misc check_wires

To make sure all wire are checked you can use this command to force all wire to be

1. Powered
    >`/mcchipmaker misc force_power_wire ON 

2. Unpowered
    >`/mcchipmaker misc force_power_wire OFF

3. Returned to the normal state
    >`/mcchipmaker misc force_power_wire Normal

After running this command run the `check_wires` command
# Internals
# Placement
First the circuit represented as a hypergraph is converted into a graph using the Clique model. Each connection in the clique has a weight of 2/n where n is the number of nodes in the clique.
For global placement a force directed algorithm is used. For detailed placement a Tetris like algorithm is used.
## Global placement
Global placement is done in `Placer.DoPlace`
First the connection matrix is made in `ConnMatrixBuilder`.
Then in every iteration The following matrix equation is solved for X and Z axis
>Ax = f + c

Where f is the spreading force. c is the force from fixed cells

This is iterated until
1. The Max iteration limit is reached
2. A cell goe out of bounds
3. A placement with no cell overlap is reached.

In every iteration the spreading force is calculated in `Placer.FixOverLapPossion`.
It calculates the force between every two cells so the complexity is (n^2 - n) where N is the number of cells.
This algorithm will be replaced with a Barnes-Hut quad tree algorithm.

## Detailed placement / Legalization
The algorithm used simply goes along the X and Z axis in both direction and selects the first valid position found.
It's not very good and will frequently choose very far from the initial position, but it works for simple cases.

# Routing
First an obstacle map is constructed. Obstacles are all ports because of torch towers that are used to connect them to wires.

For every hypergraph (wire with a single input and multiple outputs) a minimal rectilinear Steiner tree (MRST) is constructed using the flute algorithm.
The for each branch for which a straight line can not be placed a wave propagation algorithm to route around obstacles is used.
The wave probation algorithm (LeeRouter) is functionally equivalence to Dijkstra.

For each wire it tries to place it one each wire Y level starting from the bottom.
In hypergraphs each branch that intersects an obstacle is routed around using Dijkstra.
Two pin wire are routed using LeeRouter. 

Then the wires is placed on the current wire Y level if possible, if not continue go onto the next Y level repeating the process until the wire is placed.

Note:
The first wire Y level starts `Placer.Y_MAX_CELL_SIZE` blocks higher than the starting point. Each wire Y level is 2 blocks high.

Pseudocode:
````java
int y = 0;
// Obstacle map include maps for just ports and other placed wires.
var obstaclemap;

void RouteAroundObstacleAndFindYLevel()
{
   while(true)
   {
      if(hypergraph)
      {
         var routed_wire = RounteHyperGraphAroundObstacles(wire, obstaclemap, y);
         if(AttmeptPlaceWireOnLevel(routed_wire, obstaclemap, y))
            break;
      }
      else
      {
         var routed_wire = Dijakstra(wire, obstaclemap, y);
         // Obstacle map include maps for just ports and other placed wires.
         if(AttmeptPlaceWireOnLevel(routed_wire, obstaclemap, y))
            break;
      }
   }
}
````

# Redstone Wire / Repeater placement
On every corner a repeat is placed for each direction. This currently result in over use of repeaters

# Vertical connectors
For Upwards connection a simple torch tower is used taking up 1x1 blocks. Depending on the final Y level it might take up 1x2 blocks at the bottom.

Downward connectors are 1x2. Depending on the Y level the bottom part can be 2x2

# Gallery
<html lang="EN_US">
<img src="/imgs/2026-02-02_10.56.12.png" width="400" alt="">
<div></div>
<img src="/imgs/2026-02-02_10.56.24.png" width="600" alt="">
</html>
