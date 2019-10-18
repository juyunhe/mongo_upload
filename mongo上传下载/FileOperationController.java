package com.richfit.jobticket.activiti.controller;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.richfit.jobticket.activiti.service.WeekPlanActivitiService;
import com.richfit.jobticket.customerOnlinePlatform.pojo.ContractBaseMsg;
import com.richfit.jobticket.customerOnlinePlatform.pojo.SubmitOrderBaseMsg;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Encoder;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("fileoperation")
public class FileOperationController {
    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private MongoDbFactory mongoDbFactory;

    @Autowired
    private WeekPlanActivitiService weekPlanActivitiService;

    /**
     * @description: 资质文件上传
     * @date: 2019/7/8
     * @param file    //要上传的文件
     * @param conId  //合同主键  用来绑定合同 1：授权委托书 2：营业执照 3：开户许可证
     * @param custId //客户主键  用来绑定文件上传者
     * @author: zxh
     */
    @PostMapping("/flowmeter/pictures")
    public JSONObject uploadPicture(@RequestParam("file") MultipartFile file,
                                    @RequestParam("conId")String conId,
                                    @RequestParam("custId")String custId,
                                    @RequestParam("typeId")String typeId,
                                    HttpServletRequest request){
        JSONObject object = new JSONObject();
        DBObject obj = new BasicDBObject("metadata.conId", conId);
        BASE64Encoder encoder = new BASE64Encoder();
        ((BasicDBObject) obj).append("metadata.typeId",typeId);
        Query query=new BasicQuery(((BasicDBObject) obj).toString());
        GridFSFile gfsFile = gridFsTemplate.findOne(query);
        //GridFSFindIterable iter = gridFsTemplate.find(query);
        if(gfsFile!=null){
            gridFsTemplate.delete(query);
        }
        try {
                InputStream content = file.getInputStream();
                String contentType = file.getContentType();
                String fileName = file.getOriginalFilename();
                DBObject metadata = new BasicDBObject("conId", conId);
                ((BasicDBObject) metadata).append("custId", custId);
                ((BasicDBObject) metadata).append("typeId", typeId);
                ObjectId id = gridFsTemplate.store(content, fileName, contentType, metadata);
                System.out.println(id.toString());
                object.put("result","success");
                object.put("data",id);
        }catch(Exception e){
            object.put("result","fail");
            System.out.println(e.getMessage());
        }
        return object;
    }

    /**
     * @description: 申请图片文件上传
     * @date: 2019/7/18
     * @param files    //要上传的文件
     * @author: zxh
     */
    @PostMapping("/flowmeter/pictures/act")
    public JSONObject uploadPictureAct(@RequestParam("files")MultipartFile files[],
                                    @RequestParam("businessKey")String businessKey,
                                    HttpServletRequest request){
        JSONObject object = new JSONObject();
        BASE64Encoder encoder = new BASE64Encoder();
        DBObject obj = new BasicDBObject("metadata.businessKey", businessKey);
        Query query=new BasicQuery(((BasicDBObject) obj).toString());
        GridFSFindIterable iter = gridFsTemplate.find(query);
        if(iter!=null){
            gridFsTemplate.delete(query);
        }
        for (MultipartFile file:files) {
        try {
            InputStream content = file.getInputStream();
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();
            DBObject metadata = new BasicDBObject("businessKey", businessKey);
            ObjectId id = gridFsTemplate.store(content, fileName, contentType, metadata);
            System.out.println(id.toString());
            object.put("result","success");
            object.put("data",id);
        }catch(Exception e){
            object.put("result","fail");
            System.out.println(e.getMessage());
        }}
        return object;
    }

    /**
     * @description: 申请图片文件
     * @date: 2019/7/19
     * @param businessKey   //条件
     * @author: zxh
     */
    @GetMapping("/flowmeter/pictures/act/{businessKey}")
    public Map<String,Object> downloadPictureAct(@PathVariable("businessKey") String businessKey, HttpServletResponse response)throws Exception  {
        Map<String,Object> map = new HashMap<>();
        List<String> picString = new ArrayList<>();
        JSONObject json = new JSONObject();
        DBObject obj = new BasicDBObject();
        BASE64Encoder encoder = new BASE64Encoder();
        ((BasicDBObject) obj).put("metadata.businessKey",businessKey);
        Query query=new BasicQuery(((BasicDBObject) obj).toString());
        GridFSFindIterable iter = gridFsTemplate.find(query);
        MongoCursor<GridFSFile> cursor = iter.iterator();
        while(cursor.hasNext()) {
            GridFSFile gridFsdbFile = cursor.next();
            // mongo-java-driver3.x以上的版本就变成了这种方式获取
            GridFSBucket bucket = GridFSBuckets.create(mongoDbFactory.getDb());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // 获取Mongodb中文件的缓存输出流
            bucket.downloadToStream(gridFsdbFile.getId(), baos);
            String data = "data:image/png;base64,"+encoder.encode(baos.toByteArray());
            picString.add(data);
        }
        map.put("data",picString);
        map.put("size",picString.size());
        return map;
    }


    /**
     * @description: 电子签名上传维护，重复就替换
     * @date: 2019/8/27
     * @param userId    //要上传的文件
     * @author: zxh
     */
    @PostMapping("/flowmeter/pictures/Work")
    public JSONObject uploadPictureWork(@RequestParam("file") MultipartFile file,
                                    @RequestParam("userId")String userId,
                                    HttpServletRequest request){
        JSONObject object = new JSONObject();
        DBObject obj = new BasicDBObject("metadata.userId", userId);
        BASE64Encoder encoder = new BASE64Encoder();
        Query query=new BasicQuery(((BasicDBObject) obj).toString());
        GridFSFile gfsFile = gridFsTemplate.findOne(query);
        //GridFSFindIterable iter = gridFsTemplate.find(query);
        if(gfsFile!=null){
            gridFsTemplate.delete(query);
        }
        try {
            InputStream content = file.getInputStream();
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();
            DBObject metadata = new BasicDBObject("userId", userId);
            ObjectId id = gridFsTemplate.store(content, fileName, contentType, metadata);
            System.out.println(id.toString());
            object.put("result","success");
            object.put("data",id);
        }catch(Exception e){
            object.put("result","fail");
            System.out.println(e.getMessage());
        }
        return object;
    }

    /**
     * @description: 电子签名维护，删除
     * @date: 2019/8/27
     * @param userId    //要上传的文件
     * @author: zxh
     */
    @PostMapping("/flowmeter/deletename/Work")
    public Map<String,Object> deleteMyWorkName(@RequestParam("userId")String userId,
                                               HttpServletRequest request){
        Map<String,Object> map = new HashMap<>();
        JSONObject object = new JSONObject();
        DBObject obj = new BasicDBObject("metadata.userId", userId);
        BASE64Encoder encoder = new BASE64Encoder();
        Query query=new BasicQuery(((BasicDBObject) obj).toString());
        GridFSFile gfsFile = gridFsTemplate.findOne(query);
        //GridFSFindIterable iter = gridFsTemplate.find(query);
        if(gfsFile!=null){
            gridFsTemplate.delete(query);
        }
        map.put("message","succes");
        return map;
    }
    /**
     * @description: 电子签名根据历史审批人显示
     * @date: 2019/8/27
     * @param processInstanceId   //条件
     * @author: zxh
     */
    @GetMapping("/downloadmywork/act/{processInstanceId}")
    public Map<String,Object> downloadmywork(@PathVariable("processInstanceId") String processInstanceId, HttpServletResponse response)throws Exception  {
        List<Map<String,Object>> mylist = (List<Map<String,Object>>)weekPlanActivitiService.getHistoryRecord(processInstanceId).get("data");
        Map<String,Object> map = new HashMap<>();
        List<Map<String,Object>> picString = new ArrayList<>();
        JSONObject json = new JSONObject();
        for (Map<String,Object> map1:mylist) {
            DBObject obj = new BasicDBObject();
            BASE64Encoder encoder = new BASE64Encoder();
            ((BasicDBObject) obj).put("metadata.userId",map1.get("userId"));
            Query query=new BasicQuery(((BasicDBObject) obj).toString());
            GridFSFindIterable iter = gridFsTemplate.find(query);
            MongoCursor<GridFSFile> cursor = iter.iterator();
            while(cursor.hasNext()) {
                GridFSFile gridFsdbFile = cursor.next();
                // mongo-java-driver3.x以上的版本就变成了这种方式获取
                GridFSBucket bucket = GridFSBuckets.create(mongoDbFactory.getDb());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // 获取Mongodb中文件的缓存输出流
                bucket.downloadToStream(gridFsdbFile.getId(), baos);
                String data = "data:image/png;base64,"+encoder.encode(baos.toByteArray());
                Map<String,Object> m = new HashMap<>();
                m.put("result",data);
                m.put("agent",map1);
                picString.add(m);
            }
        }
        map.put("data",picString);
        map.put("size",picString.size());
        return map;
    }

    /**
     * @description: 申请图片文件
     * @date: 2019/8/27
     * @param userId   //条件
     * @author: zxh
     */
    @GetMapping("/downloadmywork/myself/{userId}")
    public Map<String,Object> downloadByUserId(@PathVariable("userId") String userId, HttpServletResponse response)throws Exception  {
        Map<String,Object> map = new HashMap<>();
        List<String> picString = new ArrayList<>();
        JSONObject json = new JSONObject();
        DBObject obj = new BasicDBObject();
        BASE64Encoder encoder = new BASE64Encoder();
        ((BasicDBObject) obj).put("metadata.userId",userId);
        Query query=new BasicQuery(((BasicDBObject) obj).toString());
        GridFSFindIterable iter = gridFsTemplate.find(query);
        MongoCursor<GridFSFile> cursor = iter.iterator();
        while(cursor.hasNext()) {
            GridFSFile gridFsdbFile = cursor.next();
            // mongo-java-driver3.x以上的版本就变成了这种方式获取
            GridFSBucket bucket = GridFSBuckets.create(mongoDbFactory.getDb());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // 获取Mongodb中文件的缓存输出流
            bucket.downloadToStream(gridFsdbFile.getId(), baos);
            String data = "data:image/png;base64,"+encoder.encode(baos.toByteArray());
            picString.add(data);
        }
        map.put("data",picString);
        map.put("size",picString.size());
        return map;
    }




    /**
     * @description: 下载
     * @date: 2019/7/8
     * @param id //记录id
     * @author: zxh
     */
    @GetMapping("/flowmeter/pictures/{id}")
    public JSONObject downloadPicture(@PathVariable String id, HttpServletResponse response)throws Exception  {
        JSONObject json = new JSONObject();
        DBObject obj = new BasicDBObject();
        BASE64Encoder encoder = new BASE64Encoder();
        ((BasicDBObject) obj).put("_id",id);
        Query query=new BasicQuery(((BasicDBObject) obj).toString());
        GridFSFindIterable iter = gridFsTemplate.find(query);
        MongoCursor<GridFSFile> cursor = iter.iterator();
        while(cursor.hasNext()) {
            GridFSFile gridFsdbFile = cursor.next();
            // mongo-java-driver3.x以上的版本就变成了这种方式获取
            GridFSBucket bucket = GridFSBuckets.create(mongoDbFactory.getDb());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // 获取Mongodb中文件的缓存输出流
            bucket.downloadToStream(gridFsdbFile.getId(), baos);
            response.setContentType(gridFsdbFile.getMetadata().get("_contentType").toString());
            response.setHeader("Content-Disposition", "attachment;filename=\"" + (new String(gridFsdbFile.getFilename().getBytes("utf-8"),"ISO8859-1")) + "\"");
            try {
                response.getOutputStream().write(baos.toByteArray());
                json.put("result","success");
            } catch (IOException e) {
                e.printStackTrace();
                json.put("result","fail");
            }
        }
        return json;
    }

    /**
     * 上传文件测试
     * @param file
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public JSONObject upload(@RequestParam("file") MultipartFile file,
                                    HttpServletRequest request){
        JSONObject object = new JSONObject();
        try {
            InputStream content = file.getInputStream();
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();
            DBObject metadata = new BasicDBObject();
            ObjectId id = gridFsTemplate.store(content, fileName, contentType, metadata);
            System.out.println(id.toString());
            object.put("result","success");
            object.put("data",id);
        }catch(Exception e){
            object.put("result","fail");
            System.out.println(e.getMessage());
        }
        return object;
    }

    /**
     *获取资质文件的id
     * @return
     */
    @GetMapping("/flowmeter/pictures/findkey")
    public Map<String,Object> downloadPictures(@RequestParam("conId")String conId){
        Map<String,Object> map =new HashMap<>();
        for (int i = 1; i <4; i++) {
            String typeId = String.valueOf(i);
            DBObject obj = new BasicDBObject("metadata.conId", conId);
            BASE64Encoder encoder = new BASE64Encoder();
            ((BasicDBObject) obj).append("metadata.typeId",typeId);
            Query query=new BasicQuery(((BasicDBObject) obj).toString());
            GridFSFile gfsFile = gridFsTemplate.findOne(query);
            if (gfsFile!=null){
                map.put("data"+i,gfsFile.getObjectId().toString());
            }
        }
        return map;
    }
}
