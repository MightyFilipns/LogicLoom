package com.mightyfilipns.chipmakermc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VCDHandler
{
    static Map<String, Integer> netname_toid = new HashMap<>();

    public void SetMap(JsonDesign.DesignModule m)
    {
        for (Map.Entry<String, JsonDesign.JsonWire> en : m.netnames.entrySet())
        {
            if (en.getValue().bits.size() == 1)
            {
                netname_toid.put(en.getKey() , en.getValue().bits.getFirst());
            }
            else
            {
                int i = 0;
                for (Integer bit : en.getValue().bits)
                {
                    netname_toid.put(en.getKey() + i, bit);
                    i++;
                }
            }
        }
    }

    public static void LoadVCD(String s)
    {
        Map<String, String> namemap = new HashMap<>();
        Map<String, Boolean> valuemap = new HashMap<>();
        List<String> d;
        try {
            d = Files.readAllLines(Path.of(s));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int i1 = d.indexOf("  $scope module SimpleCalc $end");
        i1++;
        while(!Objects.equals(d.get(i1), "  $upscope $end"))
        {
            var sp = d.get(i1).trim().split(" ");
            int size = Integer.parseInt(sp[2]);
            String vcdname = sp[3];
            String realname = sp[4];
            if (size == 1)
            {
                namemap.put(vcdname, realname);
            }
            else
            {
                for (int i = 0; i < size; i++)
                {
                    namemap.put(vcdname, realname + i);
                }
            }
            i1++;
        }
        int datastart = d.indexOf("#0");
        for (int i = datastart; i < d.size(); i++)
        {
            String cl = d.get(i);
            if (cl.charAt(0) == 'b')
            {
                var vl = cl.split(" ");
                String vcdname = vl[1];
                int i3 = 0;
                String v = vl[0].substring(1);
                for (int i2 : v.chars().toArray())
                {
                    valuemap.put(namemap.get(vcdname) + i3, i2 == '1');
                    i3++;
                }
            }
            else
            {
                boolean v = cl.charAt(0) == '1';
                String vcddump = cl.substring(1);
                String nm = namemap.get(vcddump);
                valuemap.put(nm, v);
            }
        }
        int a = 0;
    }
}
