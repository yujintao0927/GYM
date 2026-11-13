import json, pathlib, tempfile, unittest
from run import Harness
class ToolTests(unittest.TestCase):
 def harness(self):
  h=Harness.__new__(Harness);h.rows=[];h.secrets=['private-sentinel-value'];return h
 def test_expected_status_tamper_fails(self):
  h=self.harness();h.check('tampered',lambda:(201,{},{}),200);self.assertFalse(h.rows[0]['pass'])
 def test_error_secret_fails(self):
  h=self.harness();body=dict(code='X',message=h.secrets[0],requestId='id',retryable=False,details={})
  h.check('leak',lambda:(401,body,{}),401);self.assertFalse(h.rows[0]['pass'])
 def test_unicode_error_secret_fails(self):
  h=self.harness();h.secrets=['中'*24];body=dict(code='X',message=h.secrets[0],requestId='id',retryable=False,details={})
  h.check('unicode leak',lambda:(401,body,{}),401);self.assertFalse(h.rows[0]['pass'])
 def test_error_shape_fails(self):
  h=self.harness();h.check('shape',lambda:(401,{},{}),401);self.assertFalse(h.rows[0]['pass'])
 def test_correct_error_passes(self):
  h=self.harness();body=dict(code='X',message='Rejected',requestId='id',retryable=False,details={})
  h.check('correct',lambda:(401,body,{}),401,'X');self.assertTrue(h.rows[0]['pass'])
 def test_output_secret_rejected(self):
  h=self.harness();h.rows=[{'case':h.secrets[0],'pass':True}]
  with tempfile.TemporaryDirectory() as d:
   with self.assertRaises(RuntimeError):h.save(pathlib.Path(d)/'out.json')
 def test_no_store_required(self):
  h=self.harness();h.check('cache',lambda:(200,{},{}),200);self.assertFalse(h.rows[0]['pass'])
if __name__=='__main__':unittest.main()
